package com.example.cuidadiario.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.cuidadiario.AlarmAlertActivity
import com.example.cuidadiario.R
import com.example.cuidadiario.SyncManager
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MonitorAlertasService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val client = OkHttpClient()
    private var ejecutando = true

    companion object {
        const val CANAL_ID = "cuidadiario_monitor_channel"
        const val NOTIF_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()
        startForeground(NOTIF_ID, construirNotificacion())
        iniciarMonitoreo()
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                "Monitoreo CuidaDiario",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene activo el canal de telemetría y alertas del tutor"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }
    }

    private fun construirNotificacion(): Notification {
        return NotificationCompat.Builder(this, CANAL_ID)
            .setContentTitle("CuidaDiario Activo")
            .setContentText("Supervisión clínica y telemetría en tiempo real")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setOngoing(true)
            .build()
    }

    private fun iniciarMonitoreo() {
        serviceScope.launch {
            while (ejecutando) {
                try {
                    // ID de usuario activo
                    val userId = com.example.cuidadiario.SessionManager.getUserId(this@MonitorAlertasService)
                    val url = "${SyncManager.BASE_URL}/comprobar_alertas.php?usuario_id=$userId"
                    val request = Request.Builder().url(url).build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val jsonStr = response.body?.string() ?: ""
                            if (jsonStr.isNotEmpty()) {
                                // Limpiar respuesta antes del JSON
                                var cleanJsonStr = jsonStr
                                val jsonStart = cleanJsonStr.indexOf('{')
                                if (jsonStart != -1) {
                                    cleanJsonStr = cleanJsonStr.substring(jsonStart)
                                }
                                val json = JSONObject(cleanJsonStr)
                                if (json.optBoolean("hay_alerta", false)) {
                                    val mensaje = json.optString("mensaje", "¡Alerta urgente del tutor!")
                                    val medId = json.optInt("medicamento_id", 0)
                                    val nombreFarmaco = json.optString("nombre_farmaco", "Alerta Remota")
                                    val dosis = json.optString("dosis", mensaje)
                                    val fotoRuta = json.optString("foto_ruta", "")
                                    
                                    dispararPantallaAlarma(medId, nombreFarmaco, dosis, fotoRuta)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Intervalo de consulta: cada 5 segundos
                delay(5000)
            }
        }
    }

    private fun dispararPantallaAlarma(medId: Int, nombreFarmaco: String, dosis: String, fotoRuta: String) {
        val channelId = "cuidadiario_urgent_alarms"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de alta prioridad si no existe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canalExistente = notificationManager.getNotificationChannel(channelId)
            if (canalExistente == null) {
                val canal = NotificationChannel(
                    channelId,
                    "Alarmas Clínicas Prioritarias",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Disparo de alarmas en pantalla completa"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(canal)
            }
        }

        val fullScreenIntent = Intent(this, AlarmAlertActivity::class.java).apply {
            putExtra("TIPO_ALERTA", "MEDICAMENTO")
            putExtra("ITEM_ID", medId)
            putExtra("ITEM_NOMBRE", nombreFarmaco)
            putExtra("ITEM_DOSIS", dosis)
            putExtra("ITEM_IMAGE_PATH", fotoRuta)
            
            // Empaquetado redundante de datos
            putExtra("EXTRA_NOMBRE", nombreFarmaco)
            putExtra("EXTRA_DOSIS", dosis)
            putExtra("EXTRA_FOTO_RUTA", fotoRuta)
            
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % 10000).toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("¡Alerta del Tutor: $nombreFarmaco!")
            .setContentText("Dosis: $dosis")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        ejecutando = false
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}