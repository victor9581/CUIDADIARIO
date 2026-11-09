package com.example.cuidadiario

data class TaskItem(
    val id: Int,
    val name: String,
    val date: String,
    val time: String,
    val isCompleted: Boolean,
    val snoozeMinutes: Int = 10,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)