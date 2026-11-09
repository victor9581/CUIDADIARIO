package com.example.cuidadiario

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val dbHelper = DatabaseHelper(context)
            val meds = dbHelper.getAllMedications()
            meds.forEach { med ->
                val timeParts = med.startTime.split(":")
                if (timeParts.size == 2) {
                    val h = timeParts[0].toIntOrNull() ?: 8
                    val m = timeParts[1].toIntOrNull() ?: 0
                    val targetLabel = if (med.isPet) "Mascota: ${med.petName} (${med.name})" else med.name
                    AlarmSchedulerHelper.programarAlarma(
                        context = context,
                        horaSeleccionada = h,
                        minutoSeleccionado = m,
                        titulo = targetLabel,
                        mensaje = med.dose,
                        requestCode = med.id
                    )
                }
            }
        }
    }
}