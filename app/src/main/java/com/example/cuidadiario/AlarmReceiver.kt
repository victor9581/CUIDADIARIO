package com.example.cuidadiario

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.io.File

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val idDuenoAlarma = intent.getIntExtra("USUARIO_ID", -1)
        val tipoAlerta = intent.getStringExtra("TIPO_ALERTA") ?: "MEDICAMENTO"
        val itemId = intent.getIntExtra("ITEM_ID", 0)
        val itemNombre = intent.getStringExtra("ITEM_NOMBRE")
            ?: intent.getStringExtra("MED_NOMBRE")
            ?: intent.getStringExtra("EXTRA_NOMBRE")
            ?: "Recordatorio"
        val itemDosis = intent.getStringExtra("ITEM_DOSIS")
            ?: intent.getStringExtra("MED_DOSIS")
            ?: intent.getStringExtra("EXTRA_DOSIS")
            ?: ""
        val itemImagePath = intent.getStringExtra("ITEM_IMAGE_PATH")
            ?: intent.getStringExtra("EXTRA_FOTO_RUTA")
            ?: intent.getStringExtra("EXTRA_IMAGE")
            ?: ""

        val channelId = "cuidadiario_urgent_alarms"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Alarmas Clínicas Prioritarias",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Disparo de alarmas en pantalla completa"
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent que despierta a AlarmAlertActivity a pantalla completa
        val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            putExtra("USUARIO_ID", idDuenoAlarma)
            putExtra("TIPO_ALERTA", tipoAlerta)
            putExtra("ITEM_ID", itemId)
            putExtra("ITEM_NOMBRE", itemNombre)
            putExtra("ITEM_DOSIS", itemDosis)
            putExtra("ITEM_IMAGE_PATH", itemImagePath)
            putExtra("EXTRA_NOMBRE", itemNombre)
            putExtra("EXTRA_DOSIS", itemDosis)
            putExtra("EXTRA_FOTO_RUTA", itemImagePath)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            itemId + (if (tipoAlerta == "TAREA") 20000 else 10000),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (tipoAlerta == "TAREA") "Actividad Asistencial" else "¡Hora de tu Medicamento!")
            .setContentText(if (tipoAlerta == "TAREA") itemNombre else "$itemNombre - Dosis: $itemDosis")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)

        if (tipoAlerta == "MEDICAMENTO") {
            // Si es medicamento, cargar la foto del archivo para LargeIcon y BigPictureStyle
            if (!itemImagePath.isNullOrEmpty()) {
                val archivo = File(itemImagePath)
                if (archivo.exists()) {
                    val bitmap = BitmapFactory.decodeFile(archivo.absolutePath)
                    if (bitmap != null) {
                        builder.setLargeIcon(bitmap)
                        builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).setSummaryText("Dosis: $itemDosis"))
                    }
                }
            }
        } else {
            // Si es tarea, el ícono grande es el logo de la aplicación
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
            builder.setLargeIcon(logoBitmap)
        }

        notificationManager.notify(itemId, builder.build())
    }
}