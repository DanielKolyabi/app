package ru.relabs.kurjer.persistence

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import ru.relabs.kurjer.models.AddressModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel

/**
 * Created by ProOrange on 30.08.2018.
 */
@Database(entities = [AddressModel::class, TaskModel::class, TaskItemModel::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskItemDao(): TaskItemModelDao
    abstract fun taskDao(): TaskModelDao
    abstract fun addressDao(): AddressModelDao
}