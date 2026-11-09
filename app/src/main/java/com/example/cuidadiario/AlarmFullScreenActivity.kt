package com.example.cuidadiario

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

open class AlarmFullScreenActivity : AppCompatActivity() {

    // Variable integrada de sensibilidad táctil del deslizador
    private val victorThresholdFactor: Float = 0.65f

    private lateinit var imgAlarmPhoto: ImageView
    private lateinit var tvAlarmTarget: TextView
    private lateinit var tvAlarmDetail: TextView
    private lateinit var tvSwipeLeftLabel: TextView
    private lateinit var tvSwipeRightLabel: TextView
    private lateinit var rlSwipeContainer: RelativeLayout
    private lateinit var btnSwipeThumb: Button

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var locationHelper: LocationHelper

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    private var alarmType: String = "MED"
    private var itemId: Int = -1
    private var itemName: String = ""
    private var itemDetail: String = ""
    private var imagePath: String = ""
    private var snoozeMinutes: Int = 10

    private var dX = 0f
    private var initialX = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        turnScreenOnAndUnlock()
        setContentView(R.layout.activity_alarm_full_screen)

        dbHelper = DatabaseHelper(this)
        locationHelper = LocationHelper(this)

        imgAlarmPhoto = findViewById(R.id.imgAlarmPhoto)
        tvAlarmTarget = findViewById(R.id.tvAlarmTarget)
        tvAlarmDetail = findViewById(R.id.tvAlarmDetail)
        tvSwipeLeftLabel = findViewById(R.id.tvSwipeLeftLabel)
        tvSwipeRightLabel = findViewById(R.id.tvSwipeRightLabel)
        rlSwipeContainer = findViewById(R.id.rlSwipeContainer)
        btnSwipeThumb = findViewById(R.id.btnSwipeThumb)

        alarmType = intent.getStringExtra("EXTRA_TYPE") ?: "MED"
        itemId = intent.getIntExtra("EXTRA_ID", -1)
        itemName = intent.getStringExtra("EXTRA_NAME") ?: "CuidaDiario"
        itemDetail = intent.getStringExtra("EXTRA_DETAIL") ?: ""
        imagePath = intent.getStringExtra("EXTRA_IMAGE") ?: ""
        snoozeMinutes = intent.getIntExtra("EXTRA_SNOOZE", 10)

        setupUIForType()
        startAlarmSoundAndVibration()
        setupSwipeControl()
    }

    private fun setupUIForType() {
        tvAlarmTarget.text = itemName
        tvAlarmDetail.text = itemDetail

        if (alarmType == "TASK") {
            tvSwipeLeftLabel.text = "◀ En un momento"
            tvSwipeRightLabel.text = "Completado ▶"
            imgAlarmPhoto.setImageResource(android.R.drawable.ic_menu_agenda)
        } else {
            tvSwipeLeftLabel.text = "◀ Posponer 10m"
            tvSwipeRightLabel.text = "Administrar ▶"

            if (imagePath.isNotEmpty()) {
                val file = File(imagePath)
                if (file.exists()) {
                    imgAlarmPhoto.setImageURI(Uri.fromFile(file))
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeControl() {
        btnSwipeThumb.post {
            initialX = btnSwipeThumb.x
        }

        btnSwipeThumb.setOnTouchListener { view, event ->
            val parentWidth = rlSwipeContainer.width.toFloat()
            val thumbWidth = view.width.toFloat()
            val maxDragDistance = (parentWidth - thumbWidth) / 2f

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    var newX = event.rawX + dX
                    val minX = 0f
                    val maxX = parentWidth - thumbWidth
                    if (newX < minX) newX = minX
                    if (newX > maxX) newX = maxX
                    view.x = newX
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val currentX = view.x
                    val delta = currentX - initialX

                    if (delta > maxDragDistance * victorThresholdFactor) {
                        onActionCompleted()
                    } else if (delta < -maxDragDistance * victorThresholdFactor) {
                        onActionSnooze()
                    } else {
                        view.animate().x(initialX).setDuration(200).start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun onActionCompleted() {
        stopAlarmSoundAndVibration()

        val targetMedId = if (itemId != -1) {
            itemId
        } else {
            dbHelper.getAllMedications().firstOrNull()?.id ?: -1
        }

        Toast.makeText(this, "Registrando dosis y ubicación GPS...", Toast.LENGTH_SHORT).show()

        locationHelper.captureCurrentLocation { lat, lng, address ->
            if (alarmType == "TASK") {
                if (itemId != -1) {
                    dbHelper.updateTaskStatus(itemId, true, lat, lng)
                }
            } else {
                if (targetMedId != -1) {
                    dbHelper.incrementMedProgress(targetMedId, lat, lng, address)
                }
            }

            runOnUiThread {
                Toast.makeText(this, "✓ Administrado en: $address", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun onActionSnooze() {
        stopAlarmSoundAndVibration()
        val minutes = if (alarmType == "TASK") snoozeMinutes else 10
        scheduleSnooze(minutes)
        Toast.makeText(this, "Alarma pospuesta por $minutes minutos", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun scheduleSnooze(minutes: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_TYPE", alarmType)
            putExtra("EXTRA_ID", itemId)
            putExtra("EXTRA_NAME", itemName)
            putExtra("EXTRA_DETAIL", itemDetail)
            putExtra("EXTRA_IMAGE", imagePath)
            putExtra("EXTRA_SNOOZE", snoozeMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun turnScreenOnAndUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun startAlarmSoundAndVibration() {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        ringtone = RingtoneManager.getRingtone(this, soundUri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            play()
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 800, 400, 800, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarmSoundAndVibration() {
        try {
            ringtone?.stop()
            vibrator?.cancel()
        } catch (e: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSoundAndVibration()
    }
}