package com.example.cuidadiario

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TasksActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var locationHelper: LocationHelper

    private lateinit var btnBackTasks: ImageButton
    private lateinit var tvTasksPendingBadge: TextView
    private lateinit var btnFilterToday: Button
    private lateinit var btnFilterAll: Button
    private lateinit var btnAddNewTask: Button
    private lateinit var tvSectionHeader: TextView
    private lateinit var llTasksContainer: LinearLayout

    private var showOnlyToday: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        dbHelper = DatabaseHelper(this)
        locationHelper = LocationHelper(this)

        btnBackTasks = findViewById(R.id.btnBackTasks)
        tvTasksPendingBadge = findViewById(R.id.tvTasksPendingBadge)
        btnFilterToday = findViewById(R.id.btnFilterToday)
        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnAddNewTask = findViewById(R.id.btnAddNewTask)
        tvSectionHeader = findViewById(R.id.tvSectionHeader)
        llTasksContainer = findViewById(R.id.llTasksContainer)

        btnBackTasks.setOnClickListener { finish() }

        btnFilterToday.setOnClickListener {
            showOnlyToday = true
            btnFilterToday.setBackgroundColor(0xFF0D9488.toInt())
            btnFilterToday.setTextColor(Color.WHITE)
            btnFilterAll.setBackgroundColor(0xFFE2E8F0.toInt())
            btnFilterAll.setTextColor(0xFF334155.toInt())
            tvSectionHeader.text = "Tareas de Hoy"
            loadTasks()
        }

        btnFilterAll.setOnClickListener {
            showOnlyToday = false
            btnFilterAll.setBackgroundColor(0xFF0D9488.toInt())
            btnFilterAll.setTextColor(Color.WHITE)
            btnFilterToday.setBackgroundColor(0xFFE2E8F0.toInt())
            btnFilterToday.setTextColor(0xFF334155.toInt())
            tvSectionHeader.text = "Todas las Tareas Registradas"
            loadTasks()
        }

        btnAddNewTask.setOnClickListener {
            showAddTaskDialog()
        }

        loadTasks()
    }

    private fun loadTasks() {
        llTasksContainer.removeAllViews()

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val taskList = if (showOnlyToday) {
            dbHelper.getTasksByDate(todayStr)
        } else {
            dbHelper.getAllTasks()
        }

        val pendingCount = taskList.count { !it.isCompleted }
        tvTasksPendingBadge.text = "$pendingCount pendiente(s)"

        if (taskList.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = if (showOnlyToday) {
                    "No tienes tareas programadas para hoy.\nPresiona '+ AGREGAR' para planear tu día."
                } else {
                    "No hay tareas registradas en la agenda."
                }
                textSize = 14f
                setTextColor(0xFF64748B.toInt())
                setPadding(20, 32, 20, 32)
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(R.drawable.card_rounded_bg)
            }
            llTasksContainer.addView(emptyView)
            return
        }

        val inflater = LayoutInflater.from(this)

        for (task in taskList) {
            val cardView = inflater.inflate(R.layout.item_task_card, llTasksContainer, false)

            val cbTaskStatus = cardView.findViewById<CheckBox>(R.id.cbTaskStatus)
            val tvTaskName = cardView.findViewById<TextView>(R.id.tvTaskName)
            val tvTaskDateTime = cardView.findViewById<TextView>(R.id.tvTaskDateTime)
            val btnDeleteTask = cardView.findViewById<Button>(R.id.btnDeleteTask)
            val llTaskGpsContainer = cardView.findViewById<LinearLayout>(R.id.llTaskGpsContainer)
            val tvTaskLocationText = cardView.findViewById<TextView>(R.id.tvTaskLocationText)
            val btnTaskOpenMap = cardView.findViewById<Button>(R.id.btnTaskOpenMap)

            tvTaskName.text = task.name
            tvTaskDateTime.text = "📅 ${task.date} • ⏰ ${task.time}"

            cbTaskStatus.isChecked = task.isCompleted
            if (task.isCompleted) {
                tvTaskName.paintFlags = tvTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvTaskName.setTextColor(0xFF94A3B8.toInt())
            } else {
                tvTaskName.paintFlags = tvTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvTaskName.setTextColor(0xFF0F172A.toInt())
            }

            if (task.isCompleted && task.latitude != 0.0 && task.longitude != 0.0) {
                llTaskGpsContainer.visibility = View.VISIBLE
                tvTaskLocationText.text = "📍 Cumplido verificado (Lat: ${task.latitude}, Lng: ${task.longitude})"
                btnTaskOpenMap.setOnClickListener {
                    openInGoogleMaps(task.latitude, task.longitude, task.name)
                }
            } else {
                llTaskGpsContainer.visibility = View.GONE
            }

            cbTaskStatus.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    cbTaskStatus.isEnabled = false // Deshabilitar mientras se obtiene el GPS
                    Toast.makeText(this, "Registrando cumplimiento con GPS...", Toast.LENGTH_SHORT).show()
                    locationHelper.captureCurrentLocation { lat, lng, address ->
                        // Guardar en base de datos local
                        dbHelper.updateTaskStatus(task.id, true, lat, lng)

                        // Sincronizar en el servidor
                        SyncManager.sincronizarCompletarTareaGPS(
                            context = this,
                            tareaId = task.id,
                            latitud = lat,
                            longitud = lng,
                            direccion = address
                        )

                        // Actualizar estado en servidor
                        SyncManager.actualizarTareaEnServidor(task.name, task.date, true, lat, lng) { exito, _ ->
                            runOnUiThread {
                                if (exito) {
                                    Toast.makeText(this, "✓ Tarea completada y sincronizada en MySQL", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "✓ Tarea completada localmente", Toast.LENGTH_SHORT).show()
                                }
                                loadTasks()
                            }
                        }
                    }
                } else {
                    dbHelper.updateTaskStatus(task.id, false, 0.0, 0.0)
                    SyncManager.actualizarTareaEnServidor(task.name, task.date, false, 0.0, 0.0) { _, _ ->
                        runOnUiThread { loadTasks() }
                    }
                }
            }

            btnDeleteTask.setOnClickListener {
                confirmDeleteTask(task)
            }

            llTasksContainer.addView(cardView)
        }
    }

    private fun showAddTaskDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_task)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val displayWidth = resources.displayMetrics.widthPixels
        dialog.window?.setLayout(
            (displayWidth * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val etName = dialog.findViewById<EditText>(R.id.etDialogTaskName)
        val btnDate = dialog.findViewById<Button>(R.id.btnDialogTaskDate)
        val btnTime = dialog.findViewById<Button>(R.id.btnDialogTaskTime)
        val btnSave = dialog.findViewById<Button>(R.id.btnDialogSaveTask)
        val btnCancel = dialog.findViewById<Button>(R.id.btnDialogCancelTask)

        val cal = Calendar.getInstance()
        var selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        var selectedTime = "09:00"

        btnDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val chosenCal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(chosenCal.time)
                btnDate.text = "📅 Fecha seleccionada: $selectedDate"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnTime.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                val period = if (hourOfDay >= 12) "PM" else "AM"
                val displayHour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                btnTime.text = String.format("⏰ Hora seleccionada: %02d:%02d %s", displayHour, minute, period)
            }, 9, 0, false).show()
        }

        btnSave.setOnClickListener {
            val taskName = etName.text.toString().trim()
            if (taskName.isEmpty()) {
                Toast.makeText(this, "Por favor escribe qué actividad necesitas realizar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Guardar en SQLite local
            val newTaskId = dbHelper.insertTask(taskName, selectedDate, selectedTime)

            // Programar la alarma de la tarea
            AlarmSchedulerHelper.programarAlarmaTarea(
                context = this,
                tareaId = newTaskId.toInt(),
                nombreTarea = taskName,
                horaStr = selectedTime,
                fechaStr = selectedDate
            )

            // Sincronizar en servidor
            SyncManager.enviarTareaAlServidor(
                context = this@TasksActivity,
                nombre = taskName,
                fecha = selectedDate,
                hora = selectedTime
            ) { exito, _ ->
                runOnUiThread {
                    if (exito) {
                        Toast.makeText(applicationContext, "✓ Tarea guardada y sincronizada en MySQL", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "✓ Tarea guardada en teléfono", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            dialog.dismiss()
            loadTasks()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun confirmDeleteTask(task: TaskItem) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ ¿Eliminar tarea?")
            .setMessage("¿Deseas eliminar '${task.name}' de tu agenda?")
            .setPositiveButton("Sí, eliminar") { _, _ ->
                val db = dbHelper.writableDatabase
                db.delete(DatabaseHelper.TABLE_TASKS, "${DatabaseHelper.COLUMN_TASK_ID} = ?", arrayOf(task.id.toString()))
                db.close()
                Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                loadTasks()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openInGoogleMaps(lat: Double, lng: Double, taskName: String) {
        val label = Uri.encode("Cumplimiento: $taskName")
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
}