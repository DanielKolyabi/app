package ru.relabs.kurjer.persistence

/**
 * Created by ProOrange on 30.08.2018.
 */

import android.arch.persistence.room.TypeConverter
import com.google.gson.Gson
import java.util.*


class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun jsonToIntList(value: String): List<Int> {
        return Gson().fromJson(value, listOf<Int>()::class.java)
    }

    @TypeConverter
    fun intListToJSON(value: List<Int>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun stringToStringList(value: String): List<String> {
        return Gson().fromJson(value, listOf<String>()::class.java)
    }

    @TypeConverter
    fun stringListToJSON(value: List<String>): String {
        return Gson().toJson(value)
    }

}