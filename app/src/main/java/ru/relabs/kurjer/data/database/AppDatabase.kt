package ru.relabs.kurjer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.relabs.kurjer.data.database.daos.AddressEntityDao
import ru.relabs.kurjer.data.database.daos.EntranceDataEntityDao
import ru.relabs.kurjer.data.database.daos.EntranceWarningEntityDao
import ru.relabs.kurjer.data.database.daos.FirmRejectReasonDao
import ru.relabs.kurjer.data.database.daos.ReportQueryDao
import ru.relabs.kurjer.data.database.daos.SendQueryDao
import ru.relabs.kurjer.data.database.daos.StoragePhotoDao
import ru.relabs.kurjer.data.database.daos.StorageReportDao
import ru.relabs.kurjer.data.database.daos.StorageReportRequestDao
import ru.relabs.kurjer.data.database.daos.TaskEntityDao
import ru.relabs.kurjer.data.database.daos.TaskItemEntityDao
import ru.relabs.kurjer.data.database.daos.TaskItemPhotoEntityDao
import ru.relabs.kurjer.data.database.daos.TaskItemResultEntityDao
import ru.relabs.kurjer.data.database.daos.TaskItemResultEntranceEntityDao
import ru.relabs.kurjer.data.database.entities.AddressEntity
import ru.relabs.kurjer.data.database.entities.EntranceDataEntity
import ru.relabs.kurjer.data.database.entities.EntranceWarningEntity
import ru.relabs.kurjer.data.database.entities.FirmRejectReason
import ru.relabs.kurjer.data.database.entities.ReportQueryItemEntity
import ru.relabs.kurjer.data.database.entities.SendQueryItemEntity
import ru.relabs.kurjer.data.database.entities.TaskEntity
import ru.relabs.kurjer.data.database.entities.TaskItemEntity
import ru.relabs.kurjer.data.database.entities.TaskItemPhotoEntity
import ru.relabs.kurjer.data.database.entities.TaskItemResultEntity
import ru.relabs.kurjer.data.database.entities.TaskItemResultEntranceEntity
import ru.relabs.kurjer.data.database.entities.storage.StorageReportEntity
import ru.relabs.kurjer.data.database.entities.storage.StorageReportPhotoEntity
import ru.relabs.kurjer.data.database.entities.storage.StorageReportRequestEntity

/**
 * Created by ProOrange on 30.08.2018.
 */
@Database(
    entities = [AddressEntity::class, TaskEntity::class, TaskItemEntity::class,
        TaskItemPhotoEntity::class, TaskItemResultEntity::class, TaskItemResultEntranceEntity::class,
        SendQueryItemEntity::class, ReportQueryItemEntity::class, EntranceDataEntity::class,
        FirmRejectReason::class, StorageReportEntity::class, StorageReportPhotoEntity::class, StorageReportRequestEntity::class,
        EntranceWarningEntity::class],
    version = 59
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
    abstract fun storageReportDao(): StorageReportDao
    abstract fun storagePhotoDao(): StoragePhotoDao
    abstract fun storageReportRequestDao(): StorageReportRequestDao
    abstract fun entranceWarningDao(): EntranceWarningEntityDao
}