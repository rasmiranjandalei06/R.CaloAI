package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Double.formatCalories(): String {
    return "${this.toInt()} kcal"
}

fun Double.formatOneDecimal(): String {
    return String.format(Locale.getDefault(), "%.1f", this)
}

fun Long.formatDateToHumanString(): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun String.toHumanDate(): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        val date = parser.parse(this) ?: return this
        formatter.format(date)
    } catch (e: Exception) {
        this
    }
}

fun getCurrentDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}

fun getFormattedTime(): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date())
}

fun getGreetingMessage(name: String): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
    return "$greeting, ${name.ifBlank { "User" }}! 🌅"
}
