package ru.relabs.kurjer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.relabs.kurjer.data.database.daos.*
import ru.relabs.kurjer.data.database.entities.*

/**
 * Created by ProOrange on 30.08.2018.
 */
@Database(
    entities = [AddressEntity::class, TaskEntity::class, TaskItemEntity::class,
        TaskItemPhotoEntity::class, TaskItemResultEntity::class, TaskItemResultEntranceEntity::class,
        SendQueryItemEntity::class, ReportQueryItemEntity::class, EntranceDataEntity::class,
        FirmRejectReason::class], version = 48
)
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
    abstract fun entranceDataDao(): EntranceDataEntityDao
    abstract fun firmRejectReasonDao(): FirmRejectReasonDao
}