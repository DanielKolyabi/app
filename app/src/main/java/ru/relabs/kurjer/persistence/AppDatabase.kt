package ru.relabs.kurjer.persistence

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import ru.relabs.kurjer.persistence.daos.*
import ru.relabs.kurjer.persistence.entities.*

/**
 * Created by ProOrange on 30.08.2018.
 */
@Database(entities = [AddressEntity::class, TaskEntity::class, TaskItemEntity::class,
    TaskItemPhotoEntity::class, TaskItemResultEntity::class, TaskItemResultEntranceEntity::class,
    SendQueryItemEntity::class, ReportQueryItemEntity::class], version = 19)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskItemDao(): TaskItemEntityDao
    abstract fun taskDao(): TaskEntityDao
    abstract fun addressDao(): AddressEntityDao
    abstract fun photosDao(): TaskItemPhotoEntityDao
    abstract fun taskItemResultsDao(): TaskItemResultEntityDao
    abstract fun entrancesDao(): TaskItemResultEntranceEntityDao
    abstract fun sendQueryDao(): SendQueryDao
    abstract fun reportQueryDao(): ReportQueryDao

}