package com.example.cuidadiario

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var btnBackHistory: ImageButton
    private lateinit var tvHistTotalCount: TextView
    private lateinit var llHistoryContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        dbHelper = DatabaseHelper(this)

        btnBackHistory = findViewById(R.id.btnBackHistory)
        tvHistTotalCount = findViewById(R.id.tvHistTotalCount)
        llHistoryContainer = findViewById(R.id.llHistoryContainer)

        btnBackHistory.setOnClickListener { finish() }

        loadHistoryList()
    }

    private fun loadHistoryList() {
        llHistoryContainer.removeAllViews()
        val historyList = dbHelper.getAllHistoryLogs()

        tvHistTotalCount.text = "${historyList.size} registro(s)"

        if (historyList.isEmpty()) {
            val emptyCard = TextView(this).apply {
                text = "Aún no hay tomas confirmadas en la bitácora.\nCada vez que deslices la alarma o presiones 'Ya Administrado', aparecerá aquí tu registro con GPS."
                textSize = 14f
                setTextColor(0xFF64748B.toInt())
                setPadding(20, 32, 20, 32)
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(R.drawable.card_rounded_bg)
            }
            llHistoryContainer.addView(emptyCard)
            return
        }

        val inflater = LayoutInflater.from(this)

        for (item in historyList) {
            val cardView = inflater.inflate(R.layout.item_history_card, llHistoryContainer, false)

            val tvHistDateTime = cardView.findViewById<TextView>(R.id.tvHistDateTime)
            val tvHistRecipient = cardView.findViewById<TextView>(R.id.tvHistRecipient)
            val tvHistMedName = cardView.findViewById<TextView>(R.id.tvHistMedName)
            val tvHistAddress = cardView.findViewById<TextView>(R.id.tvHistAddress)
            val btnHistOpenMap = cardView.findViewById<Button>(R.id.btnHistOpenMap)

            tvHistDateTime.text = item.dateTime
            tvHistRecipient.text = item.recipient
            tvHistMedName.text = "${item.medName} • ${item.dose}"
            tvHistAddress.text = "📍 Lugar: ${item.address}"

            if (item.latitude != 0.0 && item.longitude != 0.0) {
                btnHistOpenMap.visibility = View.VISIBLE
                btnHistOpenMap.setOnClickListener {
                    openInGoogleMaps(item.latitude, item.longitude, item.medName)
                }
            } else {
                btnHistOpenMap.visibility = View.GONE
            }

            llHistoryContainer.addView(cardView)
        }
    }

    private fun openInGoogleMaps(lat: Double, lng: Double, medName: String) {
        val label = Uri.encode("Toma: $medName")
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