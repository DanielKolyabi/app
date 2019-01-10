package ru.relabs.kurjer.persistence

/**
 * Created by ProOrange on 30.08.2018.
 */

import android.arch.persistence.room.TypeConverter
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import org.joda.time.format.DateTimeFormat
import ru.relabs.kurjer.models.GPSCoordinatesModel
import java.lang.Exception
import java.util.*


class Converters {
    val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()
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
        return gson.fromJson(value, mutableListOf<Int>()::class.java)
    }

    @TypeConverter
    fun intListToJSON(value: List<Int>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun jsonToIntPairList(value: String): List<Pair<Int, Int>> {
        val list = gson.fromJson(value, mutableListOf<MutableList<Int>>()::class.java)
        return list.map{Pair(it[0], it[1])}
    }

    @TypeConverter
    fun intPairListToJSON(value: List<Pair<Int, Int>>): String {
        return gson.toJson(value.map{listOf(it.first, it.second)})
    }

    @TypeConverter
    fun stringToStringList(value: String): List<String> {
        return gson.fromJson(value, mutableListOf<String>()::class.java)
    }

    @TypeConverter
    fun stringListToJSON(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun gpsToJSON(value: GPSCoordinatesModel): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun jsonToGPS(value: String): GPSCoordinatesModel {
        val obj = gson.fromJson(value, JsonElement::class.java).asJsonObject
        val lat = obj["lat"].asDouble
        val long = obj["long"].asDouble
        val timeStr = obj["time"].asString
        Log.d("Database Conv", timeStr)

        var date = tryParseDateWithFormat("yyyy-MM-dd'T'HH:mm:ss", timeStr)
        if(date != null){
            return GPSCoordinatesModel(lat, long, date)
        }

        date = tryParseDateWithFormat("MMM d, yyyy HH:mm:ss", timeStr)
        if(date != null){
            return GPSCoordinatesModel(lat, long, date)
        }

        date = tryParseDateWithFormat("MMM d, yyyy HH:mm:ss", timeStr, Locale("ru", "RU"))
        if(date != null){
            return GPSCoordinatesModel(lat, long, date)
        }

        return GPSCoordinatesModel(lat, long, Date())
    }

    private fun tryParseDateWithFormat(formatString: String, timeString: String, locale: Locale = Locale.ENGLISH): Date? {
        try {
            val format = DateTimeFormat.forPattern(formatString).withLocale(locale)
            val time = format.parseDateTime(timeString)
            return time.toDate()
        } catch (e: Exception){
            return null
        }
    }
}