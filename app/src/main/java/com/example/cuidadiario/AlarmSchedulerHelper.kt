package com.example.cuidadiario

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmSchedulerHelper {

    fun programarAlarma(
        context: Context,
        horaSeleccionada: Int,
        minutoSeleccionado: Int,
        titulo: String,
        mensaje: String,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, horaSeleccionada) // 0 a 23
            set(Calendar.MINUTE, minutoSeleccionado)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Programar para el día siguiente si la hora ya pasó
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("USUARIO_ID", SessionManager.getUserId(context))
            putExtra("TIPO_ALERTA", "MEDICAMENTO")
            putExtra("ITEM_ID", requestCode)
            putExtra("ITEM_NOMBRE", titulo)
            putExtra("ITEM_DOSIS", mensaje)
            putExtra("MED_NOMBRE", titulo)
            putExtra("MED_DOSIS", mensaje)
            putExtra("EXTRA_NOMBRE", titulo)
            putExtra("EXTRA_DOSIS", mensaje)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun programarAlarmaTarea(
        context: Context,
        tareaId: Int,
        nombreTarea: String,
        horaStr: String,
        fechaStr: String
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val partesHora = horaStr.split(":")
            if (partesHora.size != 2) return
            val hora = partesHora[0].toIntOrNull() ?: 8
            val minuto = partesHora[1].toIntOrNull() ?: 0

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hora)
                set(Calendar.MINUTE, minuto)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("USUARIO_ID", SessionManager.getUserId(context))
                putExtra("TIPO_ALERTA", "TAREA")
                putExtra("ITEM_ID", tareaId)
                putExtra("ITEM_NOMBRE", nombreTarea)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                tareaId + 20000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}