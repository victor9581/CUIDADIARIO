package com.example.cuidadiario

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AuthActivity : AppCompatActivity() {

    private lateinit var btnTabLogin: Button
    private lateinit var btnTabRegister: Button
    private lateinit var lblAuthName: TextView
    private lateinit var etAuthName: EditText
    private lateinit var etAuthEmail: EditText
    private lateinit var etAuthPassword: EditText
    private lateinit var btnAuthSubmit: Button
    private lateinit var tvDemoMode: TextView

    private var isRegisterMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Cargar la pantalla de login/registro
        setContentView(R.layout.activity_auth)

        btnTabLogin = findViewById(R.id.btnTabLogin)
        btnTabRegister = findViewById(R.id.btnTabRegister)
        lblAuthName = findViewById(R.id.lblAuthName)
        etAuthName = findViewById(R.id.etAuthName)
        etAuthEmail = findViewById(R.id.etAuthEmail)
        etAuthPassword = findViewById(R.id.etAuthPassword)
        btnAuthSubmit = findViewById(R.id.btnAuthSubmit)
        tvDemoMode = findViewById(R.id.tvDemoMode)

        btnTabLogin.setOnClickListener { setMode(false) }
        btnTabRegister.setOnClickListener { setMode(true) }

        // Atajo para evaluadores (relleno automático instantáneo)
        tvDemoMode.setOnClickListener {
            if (isRegisterMode) {
                etAuthName.setText("Evaluador Invitado")
                val randomNum = (100..999).random()
                etAuthEmail.setText("evaluador$randomNum@cuidadiario.com")
                etAuthPassword.setText("123456")
            } else {
                etAuthEmail.setText("victor@ejemplo.com")
                etAuthPassword.setText("123456")
            }
        }

        findViewById<TextView>(R.id.tvTermsPrivacy).setOnClickListener {
            val intentWeb = Intent(Intent.ACTION_VIEW, Uri.parse("${SyncManager.BASE_URL}/privacidad.html"))
            startActivity(intentWeb)
        }

        btnAuthSubmit.setOnClickListener {
            val email = etAuthEmail.text.toString().trim()
            val pass = etAuthPassword.text.toString().trim()

            if (isRegisterMode) {
                val name = etAuthName.text.toString().trim()
                if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(this, "Completa todos los campos para registrarte", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                btnAuthSubmit.isEnabled = false
                btnAuthSubmit.text = "Creando cuenta..."

                SyncManager.registrarUsuario(name, email, pass) { exito, mensaje, usuarioJson ->
                    runOnUiThread {
                        btnAuthSubmit.isEnabled = true
                        btnAuthSubmit.text = "CREAR MI CUENTA"

                        if (exito && usuarioJson != null) {
                            val id = usuarioJson.optInt("id", 1)
                            val nom = usuarioJson.optString("nombre", name)
                            val correo = usuarioJson.optString("correo", email)
                            val code = usuarioJson.optString("codigo_familiar", "CD-1001")

                            SessionManager.saveUserSession(this, id, nom, correo, code)
                            Toast.makeText(this, "✓ Bienvenido $nom. Tu código web es: $code", Toast.LENGTH_LONG).show()
                            evaluarConfiguracionBiometrica()
                        } else {
                            Toast.makeText(this, "Error: $mensaje", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                if (email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(this, "Ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                btnAuthSubmit.isEnabled = false
                btnAuthSubmit.text = "Verificando..."

                SyncManager.iniciarSesion(email, pass) { exito, mensaje, usuarioJson ->
                    runOnUiThread {
                        btnAuthSubmit.isEnabled = true
                        btnAuthSubmit.text = "ENTRAR A CUIDADIARIO"

                        if (exito && usuarioJson != null) {
                            val id = usuarioJson.optInt("id", 1)
                            val nom = usuarioJson.optString("nombre", "Tutor")
                            val correo = usuarioJson.optString("correo", email)
                            val code = usuarioJson.optString("codigo_familiar", "CD-1001")

                            SessionManager.saveUserSession(this, id, nom, correo, code)
                            Toast.makeText(this, "✓ Hola de nuevo, $nom", Toast.LENGTH_SHORT).show()
                            evaluarConfiguracionBiometrica()
                        } else {
                            Toast.makeText(this, "Error: $mensaje", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setMode(register: Boolean) {
        isRegisterMode = register
        if (register) {
            btnTabRegister.setBackgroundColor(0xFF0D9488.toInt())
            btnTabRegister.setTextColor(Color.WHITE)
            btnTabLogin.setBackgroundColor(0xFFE2E8F0.toInt())
            btnTabLogin.setTextColor(0xFF334155.toInt())

            lblAuthName.visibility = View.VISIBLE
            etAuthName.visibility = View.VISIBLE
            btnAuthSubmit.text = "CREAR MI CUENTA"
        } else {
            btnTabLogin.setBackgroundColor(0xFF0D9488.toInt())
            btnTabLogin.setTextColor(Color.WHITE)
            btnTabRegister.setBackgroundColor(0xFFE2E8F0.toInt())
            btnTabRegister.setTextColor(0xFF334155.toInt())

            lblAuthName.visibility = View.GONE
            etAuthName.visibility = View.GONE
            btnAuthSubmit.text = "ENTRAR A CUIDADIARIO"
        }
    }

    private fun evaluarConfiguracionBiometrica() {
        // Descargar datos actualizados de este usuario desde el servidor
        SyncManager.sincronizarBajadaDesdeServidor(this) {
            runOnUiThread {
                val biometricAvailable = BiometricAuthHelper.isBiometricAvailable(this)
                val alreadyConfigured = SessionManager.isBiometricConfigured(this)

                // Solo preguntar si el teléfono soporta huella y NUNCA se le ha preguntado
                if (biometricAvailable && !alreadyConfigured) {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("🔐 Seguridad Biométrica")
                        .setMessage("¿Deseas activar el acceso rápido con huella dactilar o PIN para proteger los historiales de salud?")
                        .setPositiveButton("Activar") { _, _ ->
                            BiometricAuthHelper.showBiometricPrompt(
                                activity = this,
                                title = "Vincular Huella o PIN",
                                subtitle = "Verifica tu identidad para activar el acceso rápido",
                                onSuccess = {
                                    SessionManager.setBiometricEnabled(this, true)
                                    Toast.makeText(this, "✓ Huella/PIN configurado con éxito", Toast.LENGTH_SHORT).show()
                                    continuarAlMain()
                                },
                                onError = { mensaje ->
                                    Toast.makeText(this, "No se pudo activar: $mensaje", Toast.LENGTH_SHORT).show()
                                    SessionManager.setBiometricEnabled(this, false)
                                    continuarAlMain()
                                }
                            )
                        }
                        .setNegativeButton("Ahora no") { _, _ ->
                            // Se registra como respondido para no volver a insistir
                            SessionManager.setBiometricEnabled(this, false)
                            continuarAlMain()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    // Si ya estaba configurado o no tiene biometría, pasar directo
                    continuarAlMain()
                }
            }
        }
    }

    private fun continuarAlMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}