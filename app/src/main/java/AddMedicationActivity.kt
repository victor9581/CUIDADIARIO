package com.example.cuidadiario

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddMedicationActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var cbIsPet: CheckBox
    private lateinit var etPetName: EditText
    private lateinit var imgPreview: ImageView
    private lateinit var btnTakePhoto: Button
    private lateinit var btnSelectPhoto: Button
    private lateinit var etMedName: EditText
    private lateinit var etDose: EditText
    private lateinit var etIntervalHours: EditText
    private lateinit var btnPickStartTime: Button
    private lateinit var btnSelectStartDate: Button
    private lateinit var cbIsLifetime: CheckBox
    private lateinit var llDaysContainer: LinearLayout
    private lateinit var etTotalDays: EditText
    private lateinit var btnSaveMedication: Button
    private lateinit var btnCancel: Button

    private var selectedStartDate: String = ""
    private var selectedStartTime: String = "08:00"
    private var currentPhotoPath: String = ""
    private var photoUri: Uri? = null

    private var editMedId: Int = -1

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            imgPreview.setImageBitmap(bitmap)
            currentPhotoPath = guardarBitmapEnAlmacenamiento(bitmap)
            Toast.makeText(this, "Foto capturada correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarBitmapEnAlmacenamiento(bitmap: Bitmap): String {
        return try {
            val nombreArchivo = "med_${System.currentTimeMillis()}.jpg"
            val archivo = File(filesDir, nombreArchivo)
            val fos = FileOutputStream(archivo)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.flush()
            fos.close()
            archivo.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private val selectPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imgPreview.setImageURI(it)
            currentPhotoPath = guardarFotoInterna(this, it)
        }
    }

    private fun guardarFotoInterna(context: Context, fotoTemporalUri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(fotoTemporalUri)
            val nombreArchivo = "med_${System.currentTimeMillis()}.jpg"
            val archivoFinal = File(context.filesDir, nombreArchivo)

            val outputStream = FileOutputStream(archivoFinal)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            archivoFinal.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medication)

        dbHelper = DatabaseHelper(this)

        cbIsPet = findViewById(R.id.cbIsPet)
        etPetName = findViewById(R.id.etPetName)
        imgPreview = findViewById(R.id.imgPreview)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        etMedName = findViewById(R.id.etMedName)
        etDose = findViewById(R.id.etDose)
        etIntervalHours = findViewById(R.id.etIntervalHours)
        btnPickStartTime = findViewById(R.id.btnPickStartTime)
        btnSelectStartDate = findViewById(R.id.btnSelectStartDate)
        cbIsLifetime = findViewById(R.id.cbIsLifetime)
        llDaysContainer = findViewById(R.id.llDaysContainer)
        etTotalDays = findViewById(R.id.etTotalDays)
        btnSaveMedication = findViewById(R.id.btnSaveMedication)
        btnCancel = findViewById(R.id.btnCancel)

        selectedStartDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        cbIsPet.setOnCheckedChangeListener { _, isChecked ->
            etPetName.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        cbIsLifetime.setOnCheckedChangeListener { _, isChecked ->
            llDaysContainer.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        btnSelectStartDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                selectedStartDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(chosen.time)
                btnSelectStartDate.text = "📅 Fecha de Inicio: $selectedStartDate"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnPickStartTime.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                val period = if (hourOfDay >= 12) "PM" else "AM"
                val displayHour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                selectedStartTime = String.format("%02d:%02d", hourOfDay, minute)
                btnPickStartTime.text = String.format("⏰ Primera toma a las: [ %02d:%02d %s ]", displayHour, minute, period)
            }, 8, 0, false).show()
        }

        btnTakePhoto.setOnClickListener {
            takePhotoLauncher.launch(null)
        }

        btnSelectPhoto.setOnClickListener {
            selectPhotoLauncher.launch("image/*")
        }

        btnCancel.setOnClickListener { finish() }

        // Si es edición, cargar datos
        editMedId = intent.getIntExtra("EXTRA_EDIT_ID", -1)
        if (editMedId != -1) {
            loadMedicationForEditing(editMedId)
        }

        btnSaveMedication.setOnClickListener {
            saveMedicationProcess()
        }
    }

    private fun loadMedicationForEditing(id: Int) {
        val med = dbHelper.getMedicationById(id) ?: return
        etMedName.setText(med.name)
        etDose.setText(med.dose)
        etIntervalHours.setText(med.intervalHours.toString())
        selectedStartTime = med.startTime
        btnPickStartTime.text = "⏰ Primera toma a las: [ ${med.startTime} ]"
        selectedStartDate = med.startDate
        btnSelectStartDate.text = "📅 Fecha de Inicio: ${med.startDate}"

        if (med.isPet) {
            cbIsPet.isChecked = true
            etPetName.visibility = View.VISIBLE
            etPetName.setText(med.petName)
        }

        if (med.isLifetime) {
            cbIsLifetime.isChecked = true
            llDaysContainer.visibility = View.GONE
        } else {
            etTotalDays.setText(med.totalDays.toString())
        }

        btnSaveMedication.text = "✓ Guardar Cambios"
    }

    private fun saveMedicationProcess() {
        val name = etMedName.text.toString().trim()
        val dose = etDose.text.toString().trim()
        val intervalStr = etIntervalHours.text.toString().trim()
        val isPet = cbIsPet.isChecked
        val petName = if (isPet) etPetName.text.toString().trim() else ""
        val isLifetime = cbIsLifetime.isChecked
        val daysStr = etTotalDays.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Por favor escribe el nombre del medicamento", Toast.LENGTH_SHORT).show()
            return
        }
        if (dose.isEmpty()) {
            Toast.makeText(this, "Por favor indica la dosis (ej. 1 tableta)", Toast.LENGTH_SHORT).show()
            return
        }
        val intervalHours = intervalStr.toIntOrNull() ?: 8
        val totalDays = if (isLifetime) 365 else (daysStr.toIntOrNull() ?: 7)

        // Programar la alarma exacta con validación de horario (día actual vs mañana)
        val timeParts = selectedStartTime.split(":")
        if (timeParts.size == 2) {
            val h = timeParts[0].toIntOrNull() ?: 8
            val m = timeParts[1].toIntOrNull() ?: 0
            val targetLabel = if (isPet) "Mascota: $petName ($name)" else name
            AlarmSchedulerHelper.programarAlarma(
                context = this,
                horaSeleccionada = h,
                minutoSeleccionado = m,
                titulo = targetLabel,
                mensaje = dose,
                requestCode = (System.currentTimeMillis() % 100000).toInt()
            )
        }

        btnSaveMedication.isEnabled = false
        btnSaveMedication.text = "Guardando..."

        if (editMedId != -1) {
            // Actualizar existente
            dbHelper.updateMedication(
                id = editMedId,
                name = name,
                dose = dose,
                totalDays = totalDays,
                intervalHours = intervalHours,
                startDate = selectedStartDate,
                imagePath = currentPhotoPath,
                startTime = selectedStartTime,
                isPet = isPet,
                petName = petName,
                isLifetime = isLifetime
            )
            Toast.makeText(this, "✓ Tratamiento actualizado con éxito", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            // Guardar en SQLite local (Offline-first)
            dbHelper.insertMedication(
                name = name,
                dose = dose,
                totalDays = totalDays,
                intervalHours = intervalHours,
                startDate = selectedStartDate,
                imagePath = currentPhotoPath,
                startTime = selectedStartTime,
                isPet = isPet,
                petName = petName,
                isLifetime = isLifetime
            )

            // Sincronizar en tiempo real con MySQL
            SyncManager.enviarMedicamentoAlServidor(
                context = this@AddMedicationActivity,
                nombre = name,
                dosis = dose,
                diasTotales = totalDays,
                intervaloHoras = intervalHours,
                fechaInicio = selectedStartDate,
                horaInicio = selectedStartTime,
                esMascota = isPet,
                nombreMascota = petName,
                esDePorVida = isLifetime
            ) { exito, mensaje ->
                runOnUiThread {
                    if (exito) {
                        Toast.makeText(applicationContext, "✓ Medicamento guardado y sincronizado en MySQL", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "✓ Guardado localmente (MySQL: $mensaje)", Toast.LENGTH_LONG).show()
                    }
                    finish()
                }
            }
        }
    }
}