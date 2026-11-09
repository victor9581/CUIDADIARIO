package com.example.cuidadiario

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var locationHelper: LocationHelper
    private lateinit var tvNurseAvatar: ImageView
    private lateinit var tvNurseMessage: TextView
    private lateinit var btnOpenAddMed: Button
    private lateinit var btnGoToTasks: Button
    private lateinit var llMedicationsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                mostrarDialogoSalida()
            }
        })

        // Validar inicio de sesión
        if (!SessionManager.isLoggedIn(this)) {
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Arrancar el servicio de monitoreo en segundo plano
        val intentServicio = Intent(this, com.example.cuidadiario.services.MonitorAlertasService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentServicio)
        } else {
            startService(intentServicio)
        }

        // Autenticación biométrica antes de cargar datos
        if (SessionManager.isBiometricEnabled(this)) {
            BiometricAuthHelper.showBiometricPrompt(
                activity = this,
                title = "Acceso a CuidaDiario",
                subtitle = "Hola, ${SessionManager.getUserName(this)}. Confirma tu identidad.",
                onSuccess = {
                    cargarInterfazPrincipal()
                },
                onError = {
                    Toast.makeText(this, "Autenticación cancelada", Toast.LENGTH_SHORT).show()
                    finish() // Cierra la app si cancela la huella
                }
            )
        } else {
            cargarInterfazPrincipal()
        }
    }

    private fun cargarInterfazPrincipal() {
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        locationHelper = LocationHelper(this)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        tvNurseAvatar = findViewById(R.id.tvNurseAvatar)
        tvNurseMessage = findViewById(R.id.tvNurseMessage)
        btnOpenAddMed = findViewById(R.id.btnOpenAddMed)
        btnGoToTasks = findViewById(R.id.btnGoToTasks)
        llMedicationsContainer = findViewById(R.id.llMedicationsContainer)

        // Validar permiso de alarmas exactas en Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        tvNurseAvatar.setOnClickListener {
            dispararAlarmaPrueba()
        }

        // Configurar el botón hamburguesa en la barra superior
        supportActionBar?.apply {
            title = "CuidaDiario"
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_hamburger)
        }

        checkAppPermissions()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_meds -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_tasks -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, TasksActivity::class.java))
                }
                R.id.nav_history -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, HistoryActivity::class.java))
                }
                R.id.nav_manual -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, UserManualActivity::class.java))
                }
                R.id.nav_about -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, AboutActivity::class.java))
                }
                R.id.nav_share -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    shareAllReports()
                }
                R.id.nav_exit -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, AuthActivity::class.java))
                    finish()
                }
            }
            true
        }

        btnOpenAddMed.setOnClickListener {
            startActivity(Intent(this, AddMedicationActivity::class.java))
        }

        btnGoToTasks.setOnClickListener {
            startActivity(Intent(this, TasksActivity::class.java))
        }

        // Datos del Tutor y Vinculación Web en la pantalla principal
        val tvTutorName = findViewById<TextView?>(R.id.tvMainTutorName)
        val tvFamilyCode = findViewById<TextView?>(R.id.tvMainFamilyCode)
        val cardTutorSync = findViewById<View?>(R.id.cardTutorSync)
        val btnOpenSync = findViewById<View?>(R.id.btnOpenSyncDialog)

        val nombreUsuario = SessionManager.getUserName(this)
        val codigoFamiliar = SessionManager.getFamilyCode(this)

        tvTutorName?.text = "Tutor: $nombreUsuario"
        tvFamilyCode?.text = "Código Web: $codigoFamiliar • Toca para ver QR"

        val abrirDialogo = View.OnClickListener {
            mostrarDialogoVinculacionWeb()
        }
        cardTutorSync?.setOnClickListener(abrirDialogo)
        btnOpenSync?.setOnClickListener(abrirDialogo)

        loadAllMedications()
    }

    private fun dispararAlarmaPrueba() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("MED_NOMBRE", "Amoxicilina (Prueba)")
            putExtra("MED_DOSIS", "500 mg - 1 cápsula")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + 5000 // Exactamente en 5 segundos

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
        Toast.makeText(this, "Alarma programada en 5 segundos. Bloquea la pantalla para probar.", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarDialogoSalida() {
        AlertDialog.Builder(this)
            .setTitle("¿Deseas salir?")
            .setMessage("¿Estás seguro de que quieres salir de CuidaDiario? Tu sesión permanecerá activa.")
            .setPositiveButton("Salir") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoVinculacionWeb() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_family_sync)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val displayWidth = resources.displayMetrics.widthPixels
        dialog.window?.setLayout((displayWidth * 0.90).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        val tvName = dialog.findViewById<TextView>(R.id.tvSyncUserName)
        val tvCode = dialog.findViewById<TextView>(R.id.tvSyncFamilyCode)
        val ivQr = dialog.findViewById<ImageView>(R.id.ivSyncQrCode)
        val btnLogout = dialog.findViewById<Button>(R.id.btnSyncLogout)
        val btnClose = dialog.findViewById<Button>(R.id.btnSyncClose)

        val userName = SessionManager.getUserName(this)
        val familyCode = SessionManager.getFamilyCode(this)

        tvName.text = "Tutor: $userName"
        tvCode.text = familyCode

        // URL a la que dirigirá al escanear
        val webUrl = "${SyncManager.BASE_URL}/dashboard.php?codigo=$familyCode"

        // Generación nativa del QR (Offline e instantánea)
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(webUrl, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            ivQr.setImageBitmap(bmp)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        btnLogout.setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(this)
                .setTitle("¿Cerrar sesión?")
                .setMessage("¿Deseas desconectar esta cuenta del teléfono?")
                .setPositiveButton("Sí, cerrar") { _, _ ->
                    
                    // Cancelar alarmas pendientes registradas en el sistema
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val dbHelper = DatabaseHelper(this)
                    val meds = dbHelper.getAllMedications()
                    for (med in meds) {
                        val pendingIntent = PendingIntent.getBroadcast(
                            this,
                            med.id,
                            Intent(this, AlarmReceiver::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager.cancel(pendingIntent)
                    }

                    // Limpiar SharedPreferences y base de datos local
                    SessionManager.logout(this)
                    dbHelper.limpiarTodaLaBaseLocal()

                    // Redirigir al Login
                    val intent = Intent(this, AuthActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // Acción al tocar el botón hamburguesa
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (SessionManager.isLoggedIn(this)) {
            SyncManager.sincronizarBajadaDesdeServidor(this) {
                runOnUiThread {
                    if (::llMedicationsContainer.isInitialized) {
                        loadAllMedications()
                    }
                }
            }
        }
    }

    private fun checkAppPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1001)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun loadAllMedications() {
        llMedicationsContainer.removeAllViews()
        val meds = dbHelper.getAllMedications()

        if (meds.isEmpty()) {
            tvNurseAvatar.setImageResource(R.drawable.ic_nurse)
            tvNurseMessage.text = "Enfermera Sofía: ¡Hola! No tienes ningún tratamiento activo. Presiona '+ Registrar Nuevo Medicamento' para comenzar."

            val emptyView = TextView(this).apply {
                text = "No hay medicamentos registrados aún."
                textSize = 14f
                setTextColor(0xFF64748B.toInt())
                setPadding(16, 24, 16, 24)
            }
            llMedicationsContainer.addView(emptyView)
            return
        }

        tvNurseAvatar.setImageResource(R.drawable.ic_nurse)
        tvNurseMessage.text = "Enfermera Sofía: Tienes ${meds.size} tratamiento(s) en curso. Desliza hacia la derecha para editar o a la izquierda para borrar."

        val inflater = LayoutInflater.from(this)

        for (med in meds) {
            val cardView = inflater.inflate(R.layout.item_medication_card, llMedicationsContainer, false)

            val tvCardBadge = cardView.findViewById<TextView>(R.id.tvCardBadge)
            val btnQuickEdit = cardView.findViewById<Button>(R.id.btnQuickEdit)
            val btnQuickDelete = cardView.findViewById<Button>(R.id.btnQuickDelete)
            val imgCardPhoto = cardView.findViewById<ImageView>(R.id.imgCardPhoto)
            val tvCardName = cardView.findViewById<TextView>(R.id.tvCardName)
            val tvCardDose = cardView.findViewById<TextView>(R.id.tvCardDose)
            val tvCardInterval = cardView.findViewById<TextView>(R.id.tvCardInterval)
            val pbCardProgress = cardView.findViewById<ProgressBar>(R.id.pbCardProgress)
            val tvCardDaysStatus = cardView.findViewById<TextView>(R.id.tvCardDaysStatus)
            val tvCardLocation = cardView.findViewById<TextView>(R.id.tvCardLocation)
            val btnCardOpenMap = cardView.findViewById<Button>(R.id.btnCardOpenMap)
            val btnCardTakeDose = cardView.findViewById<Button>(R.id.btnCardTakeDose)

            if (med.isPet) {
                tvCardBadge.text = "🐾 Mascota: ${med.petName}"
                tvCardBadge.setTextColor(0xFFD97706.toInt())
            } else {
                tvCardBadge.text = "👤 Paciente"
                tvCardBadge.setTextColor(0xFF0284C7.toInt())
            }

            tvCardName.text = med.name
            tvCardDose.text = "Dosis: ${med.dose}"
            tvCardInterval.text = "⏰ Cada ${med.intervalHours} hrs (Inicio: ${med.startTime})"

            tvCardDaysStatus.text = DoseCalculator.getProgressStatusText(
                med.totalDays,
                med.daysCompleted,
                med.intervalHours,
                med.isLifetime
            )

            val dosesPerDay = (24 / med.intervalHours.coerceIn(1, 24)).coerceAtLeast(1)
            val totalDoses = med.totalDays * dosesPerDay

            // Calcula el multiplicador porcentual por dosis
            val lopezProgressFactor: Float = if (totalDoses > 0) 100f / totalDoses.toFloat() else 0f
            val percent = if (med.isLifetime) 100 else (med.daysCompleted * lopezProgressFactor).toInt().coerceIn(0, 100)
            pbCardProgress.progress = percent

            val lat = if (med.latitude != 0.0) med.latitude else 14.6349
            val lng = if (med.longitude != 0.0) med.longitude else -90.5069
            val locDisplay = if (med.address.isNotEmpty()) med.address else if (med.latitude != 0.0) "Lat: ${med.latitude}, Lng: ${med.longitude}" else "Pendiente de registro"

            tvCardLocation.text = "📍 Última toma: $locDisplay"
            btnCardOpenMap.visibility = View.VISIBLE

            btnCardOpenMap.setOnClickListener {
                openInGoogleMaps(lat, lng, med.name)
            }

            val ruta = med.imagePath
            android.util.Log.d("CuidaDiario", "Medicamento: ${med.name} -> Ruta: $ruta")

            if (!ruta.isNullOrEmpty()) {
                try {
                    val cleanPath = if (ruta.startsWith("file://")) ruta.substring(7) else ruta
                    val archivo = File(cleanPath)

                    if (archivo.exists()) {
                        val bitmap = BitmapFactory.decodeFile(archivo.absolutePath)
                        if (bitmap != null) {
                            imgCardPhoto.setImageBitmap(bitmap)
                            imgCardPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
                        } else {
                            imgCardPhoto.setImageResource(android.R.drawable.ic_menu_camera)
                        }
                    } else if (ruta.startsWith("content://")) {
                        val uri = Uri.parse(ruta)
                        val inputStream = contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream.close()
                            if (bitmap != null) {
                                imgCardPhoto.setImageBitmap(bitmap)
                                imgCardPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
                            } else {
                                imgCardPhoto.setImageResource(android.R.drawable.ic_menu_camera)
                            }
                        } else {
                            imgCardPhoto.setImageResource(android.R.drawable.ic_menu_camera)
                        }
                    } else {
                        imgCardPhoto.setImageResource(android.R.drawable.ic_menu_camera)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    imgCardPhoto.setImageResource(android.R.drawable.ic_menu_camera)
                }
            } else {
                imgCardPhoto.setImageResource(android.R.drawable.ic_menu_camera)
            }

            btnQuickEdit.setOnClickListener {
                val editIntent = Intent(this, AddMedicationActivity::class.java).apply {
                    putExtra("EXTRA_EDIT_ID", med.id)
                }
                startActivity(editIntent)
            }

            btnQuickDelete.setOnClickListener {
                confirmDeleteMedication(med)
            }

            btnCardTakeDose.setOnClickListener {
                val maxAllowedDoses = if (med.isLifetime) Int.MAX_VALUE else totalDoses
                if (med.daysCompleted < maxAllowedDoses) {
                    btnCardTakeDose.isEnabled = false
                    btnCardTakeDose.text = "Ubicando GPS..."
                    Toast.makeText(this, "Obteniendo ubicación GPS...", Toast.LENGTH_SHORT).show()
                    locationHelper.captureCurrentLocation { lat, lng, address ->
                        // Guardar en SQLite local
                        dbHelper.incrementMedProgress(med.id, lat, lng, address)

                        val recipientLabel = if (med.isPet) "🐾 Mascota: ${med.petName}" else "👤 Paciente"
                        val nowFormatted = java.text.SimpleDateFormat("dd/MM/yyyy • hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())

                        // Enviar al servidor en segundo plano
                        SyncManager.enviarTomaAlServidor(
                            context = this@MainActivity,
                            medId = med.id,
                            nombreFarmaco = med.name,
                            dosis = med.dose,
                            destinatario = recipientLabel,
                            fechaHora = nowFormatted,
                            latitud = lat,
                            longitud = lng,
                            direccion = address
                        ) { exito, mensaje ->
                            runOnUiThread {
                                if (exito) {
                                    Toast.makeText(this, "✓ Sincronizado en MySQL con éxito", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Fallo servidor: $mensaje", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        runOnUiThread {
                            loadAllMedications()
                        }
                    }
                } else {
                    Toast.makeText(this, "El tratamiento de ${med.name} ya está completado al 100%.", Toast.LENGTH_SHORT).show()
                }
            }

            var startX = 0f
            var startY = 0f
            var isDetermined = false
            var isHorizontal = false

            cardView.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        isDetermined = false
                        isHorizontal = false
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - startX
                        val deltaY = event.rawY - startY

                        if (!isDetermined) {
                            if (abs(deltaX) > 18f || abs(deltaY) > 18f) {
                                isDetermined = true
                                if (abs(deltaX) > abs(deltaY)) {
                                    isHorizontal = true
                                    v.parent.requestDisallowInterceptTouchEvent(true)
                                } else {
                                    isHorizontal = false
                                    v.parent.requestDisallowInterceptTouchEvent(false)
                                    v.animate().translationX(0f).setDuration(100).start()
                                }
                            }
                        }

                        if (isHorizontal) {
                            v.translationX = deltaX.coerceIn(-300f, 300f)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaX = event.rawX - startX
                        v.parent.requestDisallowInterceptTouchEvent(false)

                        if (isHorizontal) {
                            val threshold = 110f
                            if (deltaX > threshold) {
                                v.animate().translationX(0f).setDuration(150).start()
                                val editIntent = Intent(this, AddMedicationActivity::class.java).apply {
                                    putExtra("EXTRA_EDIT_ID", med.id)
                                }
                                startActivity(editIntent)
                            } else if (deltaX < -threshold) {
                                v.animate().translationX(0f).setDuration(150).start()
                                confirmDeleteMedication(med)
                            } else {
                                v.animate().translationX(0f).setDuration(150).start()
                            }
                        } else if (!isDetermined) {
                            v.animate().translationX(0f).setDuration(100).start()
                            showTreatmentCalendarDialog(med)
                        }
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        v.animate().translationX(0f).setDuration(150).start()
                        true
                    }
                    else -> false
                }
            }

            llMedicationsContainer.addView(cardView)
        }
    }

    private fun confirmDeleteMedication(med: Medication) {
        val targetName = if (med.isPet) "${med.petName} (${med.name})" else med.name

        AlertDialog.Builder(this)
            .setTitle("🗑️ ¿Eliminar tratamiento?")
            .setMessage("¿Estás seguro de que deseas eliminar el tratamiento de $targetName? Esta acción no se puede deshacer.")
            .setPositiveButton("Sí, eliminar") { _, _ ->
                val deleted = dbHelper.deleteMedication(med.id)
                if (deleted) {
                    Toast.makeText(this, "Tratamiento eliminado correctamente", Toast.LENGTH_SHORT).show()
                    loadAllMedications()
                } else {
                    Toast.makeText(this, "No se pudo eliminar", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openInGoogleMaps(lat: Double, lng: Double, medName: String) {
        val label = Uri.encode("Dosis: $medName")
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }

        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    private fun showTreatmentCalendarDialog(med: Medication) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_treatment_detail)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvDialogIcon = dialog.findViewById<TextView>(R.id.tvDialogIcon)
        val tvDialogTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvDialogSubtitle = dialog.findViewById<TextView>(R.id.tvDialogSubtitle)
        val tvDialogContent = dialog.findViewById<TextView>(R.id.tvDialogContent)
        val btnDialogMap = dialog.findViewById<Button>(R.id.btnDialogMap)
        val btnDialogClose = dialog.findViewById<Button>(R.id.btnDialogClose)

        if (med.isPet) {
            tvDialogIcon.text = "🐾"
            tvDialogTitle.text = "${med.petName} • ${med.name}"
            tvDialogSubtitle.text = "Tratamiento Veterinario"
        } else {
            tvDialogIcon.text = "💊"
            tvDialogTitle.text = med.name
            tvDialogSubtitle.text = "Tratamiento de Paciente"
        }

        val locText = if (med.latitude != 0.0) {
            if (med.address.isNotEmpty()) "${med.address}\n(Coordenadas: ${med.latitude}, ${med.longitude})" else "${med.latitude}, ${med.longitude}"
        } else {
            "Sin registro previo de GPS"
        }

        val dosesPerDay = (24 / med.intervalHours.coerceIn(1, 24)).coerceAtLeast(1)
        val totalDoses = med.totalDays * dosesPerDay

        val detailsText = if (med.isLifetime) {
            """
                ♾️ TRATAMIENTO DE POR VIDA
                📅 Inicio: ${med.startDate}
                ⏰ Horario: ${med.startTime} (Cada ${med.intervalHours} hrs)
                ⚖️ Dosis: ${med.dose}
                
                ✓ Tomas acumuladas: ${med.daysCompleted}
                
                📍 Última ubicación registrada:
                $locText
            """.trimIndent()
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            try {
                val date = sdf.parse(med.startDate)
                if (date != null) cal.time = date
            } catch (e: Exception) { }

            cal.add(Calendar.DAY_OF_YEAR, med.totalDays)
            val estimatedEnd = sdf.format(cal.time)

            val daysGridBuilder = StringBuilder()
            for (d in 1..med.totalDays) {
                daysGridBuilder.append("Día $d:\n")
                for (t in 1..dosesPerDay) {
                    val doseIndex = ((d - 1) * dosesPerDay) + t
                    val mark = if (doseIndex <= med.daysCompleted) "[✓ Toma $t]" else "[○ Toma $t]"
                    daysGridBuilder.append("  $mark ")
                }
                daysGridBuilder.append("\n")
            }

            """
                📅 Inicio: ${med.startDate}  |  🏁 Fin: $estimatedEnd
                ⏰ Horario: Cada ${med.intervalHours} hrs (Inicio: ${med.startTime})
                ⚖️ Dosis: ${med.dose}
                
                📊 Progreso: ${med.daysCompleted} de $totalDoses tomas ($dosesPerDay tomas al día).
                
                📍 Último GPS registrado:
                $locText
                
                BITÁCORA DE DOSIS:
                [✓] = Cumplida | [○] = Pendiente
                ----------------------------------------
                $daysGridBuilder
            """.trimIndent()
        }

        tvDialogContent.text = detailsText

        if (med.latitude != 0.0 && med.longitude != 0.0) {
            btnDialogMap.visibility = View.VISIBLE
            btnDialogMap.setOnClickListener {
                openInGoogleMaps(med.latitude, med.longitude, med.name)
            }
        } else {
            btnDialogMap.visibility = View.GONE
        }

        btnDialogClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun shareAllReports() {
        val meds = dbHelper.getAllMedications()
        if (meds.isEmpty()) {
            Toast.makeText(this, "No hay tratamientos activos para compartir", Toast.LENGTH_SHORT).show()
            return
        }

        val reportBuilder = StringBuilder()
        reportBuilder.append("📋 REPORTE DE TRATAMIENTOS - CUIDADIARIO\n")
        reportBuilder.append("------------------------------------\n")

        for (med in meds) {
            val target = if (med.isPet) "Mascota: ${med.petName}" else "Paciente"
            val dosesPerDay = (24 / med.intervalHours.coerceIn(1, 24)).coerceAtLeast(1)
            val totalDoses = med.totalDays * dosesPerDay
            val duracion = if (med.isLifetime) "De por vida" else "${med.totalDays} días ($totalDoses tomas)"
            val remainingDoses = if (med.isLifetime) "Continuo" else "${(totalDoses - med.daysCompleted).coerceAtLeast(0)} tomas"
            val loc = if (med.latitude != 0.0) {
                if (med.address.isNotEmpty()) "${med.address} (${med.latitude}, ${med.longitude})" else "${med.latitude}, ${med.longitude}"
            } else "No disponible"

            reportBuilder.append("👤 Destinatario: $target\n")
            reportBuilder.append("💊 Fármaco: ${med.name} (${med.dose})\n")
            reportBuilder.append("⏰ Horario: Cada ${med.intervalHours} hrs ($dosesPerDay tomas/día)\n")
            reportBuilder.append("📊 Duración: $duracion | Avance: ${med.daysCompleted} tomas (Faltan: $remainingDoses)\n")
            reportBuilder.append("📍 Lugar de toma: $loc\n")
            reportBuilder.append("------------------------------------\n")
        }

        reportBuilder.append("Generado por CuidaDiario.")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, reportBuilder.toString())
        }
        startActivity(Intent.createChooser(intent, "Compartir reporte médico:"))
    }
}