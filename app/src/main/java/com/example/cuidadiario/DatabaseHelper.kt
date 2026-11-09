package com.example.cuidadiario

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "cuidadiario.db"
        private const val DATABASE_VERSION = 7

        const val TABLE_MEDS = "medicamentos"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "nombre"
        const val COLUMN_DOSE = "dosis"
        const val COLUMN_TOTAL_DAYS = "dias_totales"
        const val COLUMN_INTERVAL_HOURS = "intervalo_horas"
        const val COLUMN_DAYS_COMPLETED = "dias_completados"
        const val COLUMN_START_DATE = "fecha_inicio"
        const val COLUMN_START_TIME = "hora_inicio"
        const val COLUMN_IMAGE_PATH = "ruta_foto"
        const val COLUMN_IS_PET = "es_mascota"
        const val COLUMN_PET_NAME = "nombre_mascota"
        const val COLUMN_IS_LIFETIME = "es_de_por_vida"
        const val COLUMN_LATITUDE = "latitud"
        const val COLUMN_LONGITUDE = "longitud"
        const val COLUMN_ADDRESS = "direccion"

        const val TABLE_TASKS = "tareas"
        const val COLUMN_TASK_ID = "id"
        const val COLUMN_TASK_NAME = "nombre"
        const val COLUMN_TASK_DATE = "fecha"
        const val COLUMN_TASK_TIME = "hora"
        const val COLUMN_TASK_STATUS = "completada"
        const val COLUMN_TASK_SNOOZE = "minutos_aplazado"
        const val COLUMN_TASK_LATITUDE = "latitud"
        const val COLUMN_TASK_LONGITUDE = "longitud"

        // Bitácora de tomas permanente
        const val TABLE_HISTORY = "historial_tomas"
        const val COLUMN_HIST_ID = "id"
        const val COLUMN_HIST_MED_ID = "med_id"
        const val COLUMN_HIST_MED_NAME = "med_nombre"
        const val COLUMN_HIST_DOSE = "med_dosis"
        const val COLUMN_HIST_RECIPIENT = "destinatario"
        const val COLUMN_HIST_DATE_TIME = "fecha_hora"
        const val COLUMN_HIST_LATITUDE = "latitud"
        const val COLUMN_HIST_LONGITUDE = "longitud"
        const val COLUMN_HIST_ADDRESS = "direccion"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createMedsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_MEDS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_DOSE TEXT NOT NULL,
                $COLUMN_TOTAL_DAYS INTEGER NOT NULL,
                $COLUMN_INTERVAL_HOURS INTEGER,
                $COLUMN_DAYS_COMPLETED INTEGER DEFAULT 0,
                $COLUMN_START_DATE TEXT NOT NULL,
                $COLUMN_START_TIME TEXT DEFAULT '08:00',
                $COLUMN_IMAGE_PATH TEXT,
                $COLUMN_IS_PET INTEGER DEFAULT 0,
                $COLUMN_PET_NAME TEXT DEFAULT '',
                $COLUMN_IS_LIFETIME INTEGER DEFAULT 0,
                $COLUMN_LATITUDE REAL DEFAULT 0.0,
                $COLUMN_LONGITUDE REAL DEFAULT 0.0,
                $COLUMN_ADDRESS TEXT DEFAULT ''
            )
        """.trimIndent()

        val createTasksTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_TASKS (
                $COLUMN_TASK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TASK_NAME TEXT NOT NULL,
                $COLUMN_TASK_DATE TEXT NOT NULL,
                $COLUMN_TASK_TIME TEXT NOT NULL,
                $COLUMN_TASK_STATUS INTEGER DEFAULT 0,
                $COLUMN_TASK_SNOOZE INTEGER DEFAULT 10,
                $COLUMN_TASK_LATITUDE REAL DEFAULT 0.0,
                $COLUMN_TASK_LONGITUDE REAL DEFAULT 0.0
            )
        """.trimIndent()

        val createHistoryTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_HISTORY (
                $COLUMN_HIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_HIST_MED_ID INTEGER,
                $COLUMN_HIST_MED_NAME TEXT NOT NULL,
                $COLUMN_HIST_DOSE TEXT NOT NULL,
                $COLUMN_HIST_RECIPIENT TEXT NOT NULL,
                $COLUMN_HIST_DATE_TIME TEXT NOT NULL,
                $COLUMN_HIST_LATITUDE REAL DEFAULT 0.0,
                $COLUMN_HIST_LONGITUDE REAL DEFAULT 0.0,
                $COLUMN_HIST_ADDRESS TEXT DEFAULT ''
            )
        """.trimIndent()

        db?.execSQL(createMedsTable)
        db?.execSQL(createTasksTable)
        db?.execSQL(createHistoryTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MEDS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        onCreate(db)
    }

    // Asegura que la tabla exista antes de cualquier operación
    private fun ensureHistoryTableExists(db: SQLiteDatabase) {
        val createHistoryTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_HISTORY (
                $COLUMN_HIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_HIST_MED_ID INTEGER,
                $COLUMN_HIST_MED_NAME TEXT NOT NULL,
                $COLUMN_HIST_DOSE TEXT NOT NULL,
                $COLUMN_HIST_RECIPIENT TEXT NOT NULL,
                $COLUMN_HIST_DATE_TIME TEXT NOT NULL,
                $COLUMN_HIST_LATITUDE REAL DEFAULT 0.0,
                $COLUMN_HIST_LONGITUDE REAL DEFAULT 0.0,
                $COLUMN_HIST_ADDRESS TEXT DEFAULT ''
            )
        """.trimIndent()
        db.execSQL(createHistoryTable)
    }

    fun insertMedication(
        name: String,
        dose: String,
        totalDays: Int,
        intervalHours: Int,
        startDate: String,
        imagePath: String,
        startTime: String = "08:00",
        isPet: Boolean = false,
        petName: String = "",
        isLifetime: Boolean = false,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        address: String = ""
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_DOSE, dose)
            put(COLUMN_TOTAL_DAYS, totalDays)
            put(COLUMN_INTERVAL_HOURS, intervalHours)
            put(COLUMN_DAYS_COMPLETED, 0)
            put(COLUMN_START_DATE, startDate)
            put(COLUMN_START_TIME, startTime)
            put(COLUMN_IMAGE_PATH, imagePath)
            put(COLUMN_IS_PET, if (isPet) 1 else 0)
            put(COLUMN_PET_NAME, petName)
            put(COLUMN_IS_LIFETIME, if (isLifetime) 1 else 0)
            put(COLUMN_LATITUDE, latitude)
            put(COLUMN_LONGITUDE, longitude)
            put(COLUMN_ADDRESS, address)
        }
        val result = db.insert(TABLE_MEDS, null, values)
        db.close()
        return result
    }

    fun updateMedication(
        id: Int,
        name: String,
        dose: String,
        totalDays: Int,
        intervalHours: Int,
        startDate: String,
        imagePath: String,
        startTime: String = "08:00",
        isPet: Boolean = false,
        petName: String = "",
        isLifetime: Boolean = false
    ): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_DOSE, dose)
            put(COLUMN_TOTAL_DAYS, totalDays)
            put(COLUMN_INTERVAL_HOURS, intervalHours)
            put(COLUMN_START_DATE, startDate)
            put(COLUMN_START_TIME, startTime)
            if (imagePath.isNotEmpty()) {
                put(COLUMN_IMAGE_PATH, imagePath)
            }
            put(COLUMN_IS_PET, if (isPet) 1 else 0)
            put(COLUMN_PET_NAME, petName)
            put(COLUMN_IS_LIFETIME, if (isLifetime) 1 else 0)
        }
        val result = db.update(TABLE_MEDS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
        return result > 0
    }

    fun deleteMedication(medId: Int): Boolean {
        val db = this.writableDatabase
        val result = db.delete(TABLE_MEDS, "$COLUMN_ID = ?", arrayOf(medId.toString()))
        db.close()
        return result > 0
    }

    fun getMedicationById(medId: Int): Medication? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MEDS WHERE $COLUMN_ID = ?", arrayOf(medId.toString()))
        var med: Medication? = null

        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
            val dose = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DOSE))
            val totalDays = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_DAYS))
            val intervalHours = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INTERVAL_HOURS))
            val daysCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DAYS_COMPLETED))
            val startDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_DATE))
            val startTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_TIME)) ?: "08:00"
            val imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)) ?: ""
            val isPet = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PET)) == 1
            val petName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PET_NAME)) ?: ""
            val isLifetime = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_LIFETIME)) == 1
            val lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE))
            val lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE))
            val address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)) ?: ""

            med = Medication(
                id, name, dose, totalDays, intervalHours, daysCompleted,
                startDate, startTime, imagePath, isPet, petName, isLifetime, lat, lng, address
            )
        }
        cursor.close()
        db.close()
        return med
    }

    // Obtener un medicamento específico por su ID local
    fun obtenerMedicamentoPorId(id: Int): android.database.Cursor? {
        val db = this.readableDatabase
        return try {
            db.rawQuery("SELECT $COLUMN_NAME AS nombre, $COLUMN_DOSE AS dosis, $COLUMN_IMAGE_PATH AS foto_path FROM $TABLE_MEDS WHERE $COLUMN_ID = ?", arrayOf(id.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAllMedications(): ArrayList<Medication> {
        val list = ArrayList<Medication>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MEDS ORDER BY $COLUMN_ID DESC", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                val dose = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DOSE))
                val totalDays = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_DAYS))
                val intervalHours = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INTERVAL_HOURS))
                val daysCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DAYS_COMPLETED))
                val startDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_DATE))
                val startTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_TIME)) ?: "08:00"
                val imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)) ?: ""
                val isPet = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PET)) == 1
                val petName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PET_NAME)) ?: ""
                val isLifetime = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_LIFETIME)) == 1
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE))
                val lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)) ?: ""

                list.add(
                    Medication(
                        id, name, dose, totalDays, intervalHours, daysCompleted,
                        startDate, startTime, imagePath, isPet, petName, isLifetime, lat, lng, address
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun incrementMedProgress(medId: Int, latitude: Double = 0.0, longitude: Double = 0.0, address: String = ""): Boolean {
        val med = getMedicationById(medId) ?: return false

        val db = this.writableDatabase
        ensureHistoryTableExists(db)

        db.execSQL("UPDATE $TABLE_MEDS SET $COLUMN_DAYS_COMPLETED = $COLUMN_DAYS_COMPLETED + 1 WHERE $COLUMN_ID = $medId")

        val cvMed = ContentValues().apply {
            put(COLUMN_LATITUDE, latitude)
            put(COLUMN_LONGITUDE, longitude)
            if (address.isNotEmpty()) {
                put(COLUMN_ADDRESS, address)
            }
        }
        db.update(TABLE_MEDS, cvMed, "$COLUMN_ID = ?", arrayOf(medId.toString()))

        val nowFormatted = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.getDefault()).format(Date())
        val recipientLabel = if (med.isPet) "🐾 Mascota: ${med.petName}" else "👤 Paciente"

        val cvHist = ContentValues().apply {
            put(COLUMN_HIST_MED_ID, med.id)
            put(COLUMN_HIST_MED_NAME, med.name)
            put(COLUMN_HIST_DOSE, med.dose)
            put(COLUMN_HIST_RECIPIENT, recipientLabel)
            put(COLUMN_HIST_DATE_TIME, nowFormatted)
            put(COLUMN_HIST_LATITUDE, latitude)
            put(COLUMN_HIST_LONGITUDE, longitude)
            put(COLUMN_HIST_ADDRESS, if (address.isNotEmpty()) address else "Ubicación confirmada")
        }
        db.insert(TABLE_HISTORY, null, cvHist)

        db.close()
        return true
    }

    fun getAllHistoryLogs(): ArrayList<HistoryItem> {
        val list = ArrayList<HistoryItem>()
        val db = this.writableDatabase
        ensureHistoryTableExists(db)

        val cursor = db.rawQuery("SELECT * FROM $TABLE_HISTORY ORDER BY $COLUMN_HIST_ID DESC", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HIST_ID))
                val medId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HIST_MED_ID))
                val medName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HIST_MED_NAME))
                val dose = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HIST_DOSE))
                val recipient = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HIST_RECIPIENT))
                val dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HIST_DATE_TIME))
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HIST_LATITUDE))
                val lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HIST_LONGITUDE))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HIST_ADDRESS)) ?: ""

                list.add(HistoryItem(id, medId, medName, dose, recipient, dateTime, lat, lng, address))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun insertTask(
        name: String,
        date: String,
        time: String,
        snoozeMinutes: Int = 10,
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TASK_NAME, name)
            put(COLUMN_TASK_DATE, date)
            put(COLUMN_TASK_TIME, time)
            put(COLUMN_TASK_STATUS, 0)
            put(COLUMN_TASK_SNOOZE, snoozeMinutes)
            put(COLUMN_TASK_LATITUDE, latitude)
            put(COLUMN_TASK_LONGITUDE, longitude)
        }
        val result = db.insert(TABLE_TASKS, null, values)
        db.close()
        return result
    }

    fun getAllTasks(): ArrayList<TaskItem> {
        val list = ArrayList<TaskItem>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TASKS ORDER BY $COLUMN_TASK_DATE ASC, $COLUMN_TASK_TIME ASC", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_NAME))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_DATE))
                val time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_TIME))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_STATUS))
                val snooze = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_SNOOZE))
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TASK_LATITUDE))
                val lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TASK_LONGITUDE))

                list.add(TaskItem(id, name, date, time, status == 1, snooze, lat, lng))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun getTasksByDate(filterDate: String): ArrayList<TaskItem> {
        val list = ArrayList<TaskItem>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TASKS WHERE $COLUMN_TASK_DATE = ? ORDER BY $COLUMN_TASK_TIME ASC", arrayOf(filterDate))

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_NAME))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_DATE))
                val time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_TIME))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_STATUS))
                val snooze = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_SNOOZE))
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TASK_LATITUDE))
                val lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TASK_LONGITUDE))

                list.add(TaskItem(id, name, date, time, status == 1, snooze, lat, lng))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun updateTaskStatus(taskId: Int, isCompleted: Boolean, latitude: Double = 0.0, longitude: Double = 0.0) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TASK_STATUS, if (isCompleted) 1 else 0)
            if (latitude != 0.0 || longitude != 0.0) {
                put(COLUMN_TASK_LATITUDE, latitude)
                put(COLUMN_TASK_LONGITUDE, longitude)
            }
        }
        db.update(TABLE_TASKS, values, "$COLUMN_TASK_ID = ?", arrayOf(taskId.toString()))
        db.close()
    }

    fun limpiarTodaLaBaseLocal() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_MEDS")
        db.execSQL("DELETE FROM $TABLE_TASKS")
        db.execSQL("DELETE FROM $TABLE_HISTORY")
        db.close()
    }

    fun incrementarToma(medicamentoId: Int) {
        try {
            val db = this.writableDatabase
            db.execSQL(
                "UPDATE $TABLE_MEDS SET $COLUMN_DAYS_COMPLETED = $COLUMN_DAYS_COMPLETED + 1 WHERE $COLUMN_ID = ?",
                arrayOf(medicamentoId.toString())
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}