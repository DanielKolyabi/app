package ru.relabs.kurjer.persistence

/**
 * Created by ProOrange on 30.08.2018.
 */

import android.arch.persistence.room.TypeConverter
import com.google.gson.Gson
import ru.relabs.kurjer.models.GPSCoordinatesModel
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
        return Gson().fromJson(value, mutableListOf<Int>()::class.java)
    }

    @TypeConverter
    fun intListToJSON(value: List<Int>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun jsonToIntPairList(value: String): List<Pair<Int, Int>> {
        val list = Gson().fromJson(value, mutableListOf<MutableList<Int>>()::class.java)
        return list.map{Pair(it[0], it[1])}
    }

    @TypeConverter
    fun intPairListToJSON(value: List<Pair<Int, Int>>): String {
        return Gson().toJson(value.map{listOf(it.first, it.second)})
    }

    @TypeConverter
    fun stringToStringList(value: String): List<String> {
        return Gson().fromJson(value, mutableListOf<String>()::class.java)
    }

    @TypeConverter
    fun stringListToJSON(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun gpsToJSON(value: GPSCoordinatesModel): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun jsonToGPS(value: String): GPSCoordinatesModel {
        return Gson().fromJson(value, GPSCoordinatesModel::class.java)
    }
}