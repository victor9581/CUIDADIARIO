package com.example.cuidadiario

object DoseCalculator {

    fun calculateScheduleList(startTime: String, intervalHours: Int): List<String> {
        val list = mutableListOf<String>()
        val parts = startTime.split(":")
        if (parts.size != 2) return list

        val startH = parts[0].toIntOrNull() ?: 8
        val startM = parts[1].toIntOrNull() ?: 0
        val validInterval = intervalHours.coerceIn(1, 24)
        val dosesPerDay = (24 / validInterval).coerceAtLeast(1)

        for (i in 0 until dosesPerDay) {
            val totalMinutes = (startH * 60 + startM) + (i * validInterval * 60)
            val h = (totalMinutes / 60) % 24
            val m = totalMinutes % 60
            list.add(String.format("%02d:%02d", h, m))
        }
        return list
    }

    fun getProgressStatusText(totalDays: Int, dosesCompleted: Int, intervalHours: Int, isLifetime: Boolean): String {
        if (isLifetime) {
            return "Tratamiento continuo • $dosesCompleted tomas registradas"
        }

        val validInterval = intervalHours.coerceIn(1, 24)
        val dosesPerDay = (24 / validInterval).coerceAtLeast(1)
        val totalDoses = totalDays * dosesPerDay

        if (dosesCompleted >= totalDoses) {
            return "✓ ¡Tratamiento completado! ($totalDays de $totalDays días)"
        }

        val currentDay = (dosesCompleted / dosesPerDay) + 1
        val doseOfToday = (dosesCompleted % dosesPerDay) + 1
        val daysRemaining = (totalDays - (dosesCompleted / dosesPerDay)).coerceAtLeast(0)

        return "Día $currentDay de $totalDays (Toma $doseOfToday de $dosesPerDay) • Faltan $daysRemaining días"
    }
}