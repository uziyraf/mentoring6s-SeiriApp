package com.example.data.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun currentTimestamp(): String = dateFormat.format(Date())
    fun currentDate(): String = dateOnlyFormat.format(Date())
}
