package ru.relabs.kurjer.utils

import java.util.*

/**
 * Created by Daniil Kurchanov on 07.01.2020.
 */
fun currentTimestamp(): Long {
    return Calendar.getInstance(TimeZone.getTimeZone("GMT+3:00")).time.time/1000
}