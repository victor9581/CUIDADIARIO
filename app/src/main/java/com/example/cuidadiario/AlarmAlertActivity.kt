package com.example.cuidadiario

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmAlertActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var tipoAlerta: String = "MEDICAMENTO"
    private var itemId: Int = 0
    private var itemNombre: String = ""
    private var itemDosis: String = ""
    private var imagePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener la sesión activa en el teléfono
        val idSesionActiva = SessionManager.getUserId(this)
        val isSesionIniciada = SessionManager.isLoggedIn(this)

        // Obtener el dueño de la alarma programada
        val idDuenoAlarma = intent.getIntExtra("USUARIO_ID", -1)

        // Validación de sesión y pertenencia de la alarma
        if (!isSesionIniciada || (idDuenoAlarma != -1 && idDuenoAlarma != idSesionActiva)) {
            finish()
            return
        }

        // Despertar la pantalla y omitir el bloqueo para emergencias médicas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_alarm_alert)

        tipoAlerta = intent.getStringExtra("TIPO_ALERTA") ?: "MEDICAMENTO"
        itemId = intent.getIntExtra("ITEM_ID", 0)

        val rutaFoto = intent.getStringExtra("EXTRA_FOTO_RUTA")
            ?: intent.getStringExtra("EXTRA_FOTO_URI")
            ?: intent.getStringExtra("ITEM_IMAGE_PATH")
            ?: ""
        android.util.Log.d("ALARMA_FOTO", "Valor recibido: '$rutaFoto'")

        // Cargar datos reales directamente desde SQLite
        cargarDatosDesdeBaseDeDatos()

        configurarVista()
        iniciarSonido()
        configurarBotones()
    }

    private fun cargarDatosDesdeBaseDeDatos() {
        itemNombre = intent.getStringExtra("ITEM_NOMBRE")
            ?: intent.getStringExtra("EXTRA_NOMBRE")
            ?: intent.getStringExtra("MED_NOMBRE")
            ?: ""
        itemDosis = intent.getStringExtra("ITEM_DOSIS")
            ?: intent.getStringExtra("EXTRA_DOSIS")
            ?: intent.getStringExtra("MED_DOSIS")
            ?: ""
        imagePath = intent.getStringExtra("ITEM_IMAGE_PATH")
            ?: intent.getStringExtra("EXTRA_FOTO_RUTA")
            ?: intent.getStringExtra("EXTRA_IMAGE")
            ?: ""

        if (tipoAlerta == "MEDICAMENTO" && itemId > 0) {
            val dbHelper = DatabaseHelper(this)
            val cursor = dbHelper.obtenerMedicamentoPorId(itemId)
            cursor?.use {
                if (it.moveToFirst()) {
                    val indexNombre = it.getColumnIndex("nombre")
                    val indexDosis = it.getColumnIndex("dosis")
                    val indexFoto = it.getColumnIndex("foto_path")

                    val dbNombre = if (indexNombre != -1) it.getString(indexNombre) else null
                    val dbDosis = if (indexDosis != -1) it.getString(indexDosis) else null
                    val dbFoto = if (indexFoto != -1) it.getString(indexFoto) else null

                    if (!dbNombre.isNullOrEmpty()) itemNombre = dbNombre
                    if (!dbDosis.isNullOrEmpty()) itemDosis = dbDosis
                    if (!dbFoto.isNullOrEmpty()) imagePath = dbFoto
                }
            }
            
            // Búsqueda local por nombre si la ruta está vacía
            if (imagePath.isEmpty() && itemNombre.isNotEmpty()) {
                val dbCursor = dbHelper.readableDatabase.rawQuery("SELECT ruta_foto FROM medicamentos WHERE nombre LIKE ?", arrayOf("%$itemNombre%"))
                if (dbCursor.moveToFirst()) {
                    val idx = dbCursor.getColumnIndex("ruta_foto")
                    if (idx != -1) {
                        imagePath = dbCursor.getString(idx) ?: ""
                    }
                }
                dbCursor.close()
            }
        } else if (tipoAlerta == "TAREA") {
            if (itemNombre.isEmpty() || itemNombre == "Recordatorio") {
                itemNombre = intent.getStringExtra("ITEM_NOMBRE")
                    ?: intent.getStringExtra("EXTRA_NOMBRE")
                    ?: "Actividad Asistencial"
            }
        }
    }

    private fun configurarVista() {
        val tvCategory = findViewById<TextView>(R.id.tvAlarmCategoryBadge)
        val tvTitle = findViewById<TextView>(R.id.tvAlarmMainTitle)
        val tvName = findViewById<TextView>(R.id.tvAlarmItemName)
        val tvSubtitle = findViewById<TextView>(R.id.tvAlarmItemSubtitle)
        val ivPhoto = findViewById<ImageView>(R.id.ivAlarmIllustration)
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmarAccion)

        if (tipoAlerta == "TAREA") {
            tvCategory.text = "AGENDA ASISTENCIAL"
            tvTitle.text = "RECUERDA QUE DEBES:"
            tvName.text = itemNombre
            tvSubtitle.text = "Actividad programada en tu agenda"
            btnConfirmar.text = "✓ YA REALICÉ LA TAREA"

            // En tareas se utiliza siempre el logo de la app / enfermera
            val nurseResId = resources.getIdentifier("enfermera", "drawable", packageName)
            if (nurseResId != 0) {
                ivPhoto.setImageResource(nurseResId)
            } else {
                ivPhoto.setImageResource(R.mipmap.ic_launcher)
            }
        } else {
            tvCategory.text = "TRATAMIENTO FARMACOLÓGICO"
            tvTitle.text = "¡HORA DE TU MEDICAMENTO!"
            tvName.text = if (itemNombre.isNotEmpty()) itemNombre else "Medicamento"
            tvSubtitle.text = if (itemDosis.isNotEmpty()) "Dosis prescrita: $itemDosis" else "Dosis programada"
            btnConfirmar.text = "✓ YA ME LO TOMÉ"

            android.util.Log.d("FOTO_TEST", "Ruta: '$imagePath'")

            // Cargar la fotografía del medicamento
            var fotoCargada = false
            if (!imagePath.isNullOrEmpty()) {
                try {
                    val file = File(imagePath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            ivPhoto.setImageBitmap(bitmap)
                            ivPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
                            fotoCargada = true
                        }
                    } else if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                        try {
                            ivPhoto.setImageURI(Uri.parse(imagePath))
                            ivPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
                            fotoCargada = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        val urlRemota = if (imagePath.startsWith("http")) imagePath else "${SyncManager.BASE_URL}/img/$imagePath"
                        Thread {
                            try {
                                val url = java.net.URL(urlRemota)
                                val conn = url.openConnection() as java.net.HttpURLConnection
                                conn.connectTimeout = 5000
                                conn.readTimeout = 5000
                                if (conn.responseCode == 200) {
                                    val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                                    if (bitmap != null) {
                                        runOnUiThread {
                                            ivPhoto.setImageBitmap(bitmap)
                                            ivPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
                                        }
                                    }
                                }
                                conn.disconnect()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }.start()
                        fotoCargada = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fallback a imagen por defecto
            if (!fotoCargada) {
                val icPlaceholder = resources.getIdentifier("ic_camera_placeholder", "drawable", packageName)
                if (icPlaceholder != 0) {
                    ivPhoto.setImageResource(icPlaceholder)
                } else {
                    ivPhoto.setImageResource(R.mipmap.ic_launcher)
                }
            }
        }
    }

    private fun configurarBotones() {
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmarAccion)
        val btnPosponer = findViewById<Button>(R.id.btnPosponerAccion)

        // Clic en Confirmar
        btnConfirmar.setOnClickListener {
            // Deshabilitar botones mientras se procesa
            btnConfirmar.isEnabled = false
            btnPosponer.isEnabled = false
            btnConfirmar.text = "Ubicando GPS..."
            
            vibrarDispositivo()
            detenerSonido()

            val locationHelper = LocationHelper(this)
            val fechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            locationHelper.captureCurrentLocation { lat, lng, address ->
                val realLat = if (lat != 0.0) lat else 14.6349
                val realLng = if (lng != 0.0) lng else -90.5069
                val realAddr = if (address.isNotEmpty() && !address.startsWith("Ubicación")) address else "Ubicación confirmada"

                if (tipoAlerta == "TAREA") {
                    val dbHelper = DatabaseHelper(this)
                    dbHelper.updateTaskStatus(itemId, true, realLat, realLng)

                    SyncManager.sincronizarCompletarTareaGPS(
                        context = this,
                        tareaId = itemId,
                        latitud = realLat,
                        longitud = realLng,
                        direccion = realAddr
                    )
                    runOnUiThread {
                        Toast.makeText(this, "✓ Actividad completada con GPS", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    val dbHelper = DatabaseHelper(this)
                    dbHelper.incrementMedProgress(itemId, realLat, realLng, realAddr)

                    SyncManager.enviarTomaServidor(
                        context = this,
                        medicamentoId = itemId,
                        nombreFarmaco = itemNombre,
                        dosis = itemDosis,
                        destinatario = "Paciente",
                        latitud = realLat,
                        longitud = realLng,
                        direccion = realAddr,
                        fechaHora = fechaHora
                    )

                    SyncManager.registrarTomaCompleta(
                        context = this,
                        medicamentoId = itemId,
                        nombreFarmaco = itemNombre,
                        dosis = itemDosis,
                        latitud = realLat,
                        longitud = realLng,
                        direccionTexto = realAddr
                    )

                    SyncManager.sincronizarTomaGPS(
                        context = this,
                        medicamentoId = itemId,
                        nombre = itemNombre,
                        dosis = itemDosis,
                        destinatario = "Paciente",
                        fechaHora = fechaHora,
                        latitud = realLat,
                        longitud = realLng,
                        direccion = realAddr
                    )

                    runOnUiThread {
                        Toast.makeText(this, "✓ Dosis registrada con GPS", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }

        // Clic en Posponer 10 Minutos
        btnPosponer.setOnClickListener {
            vibrarDispositivo()
            detenerSonido()

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intentAlarma = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("TIPO_ALERTA", tipoAlerta)
                putExtra("ITEM_ID", itemId)
                putExtra("ITEM_NOMBRE", itemNombre)
                putExtra("ITEM_DOSIS", itemDosis)
                putExtra("ITEM_IMAGE_PATH", imagePath)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                itemId + 80000,
                intentAlarma,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val diezMinutosEnMs = System.currentTimeMillis() + (10 * 60 * 1000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, diezMinutosEnMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, diezMinutosEnMs, pendingIntent)
            }

            Toast.makeText(this, "⏰ Te recordaremos en 10 minutos", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun vibrarDispositivo() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun iniciarSonido() {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun detenerSonido() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        detenerSonido()
        super.onDestroy()
    }
}