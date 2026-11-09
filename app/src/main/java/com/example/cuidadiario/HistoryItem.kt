package com.example.cuidadiario

data class HistoryItem(
    val id: Int,
    val medId: Int,
    val medName: String,
    val dose: String,
    val recipient: String,
    val dateTime: String,
    val latitude: Double,
    val longitude: Double,
    val address: String
)