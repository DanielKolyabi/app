package ru.relabs.kurjer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by ProOrange on 31.08.2018.
 */
@Entity(tableName = "addresses")
data class AddressEntity(
        @PrimaryKey
        var id: Int,
        @ColumnInfo(name = "city")
        var city: String,
        @ColumnInfo(name = "street")
        var street: String,
        @ColumnInfo(name = "house")
        var house: Int,
        @ColumnInfo(name = "house_name")
        var houseName: String,
        @ColumnInfo(name = "gps_lat")
        var gpsLat: Double,
        @ColumnInfo(name = "gps_long")
        var gpsLong: Double
)