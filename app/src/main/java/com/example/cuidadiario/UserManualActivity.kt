package com.example.cuidadiario

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class UserManualActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_manual)

        val btnBack = findViewById<ImageButton>(R.id.btnBackManual)
        btnBack.setOnClickListener { finish() }
    }
}