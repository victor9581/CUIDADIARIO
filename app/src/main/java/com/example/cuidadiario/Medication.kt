package com.example.cuidadiario

data class Medication(
    val id: Int,
    val name: String,
    val dose: String,
    val totalDays: Int,
    val intervalHours: Int,
    val daysCompleted: Int,
    val startDate: String,
    val startTime: String = "08:00",
    val imagePath: String = "",
    val isPet: Boolean = false,
    val petName: String = "",
    val isLifetime: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = ""
)