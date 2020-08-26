package ru.relabs.kurjer.persistence

import android.os.Environment
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.entities.ReportQueryItemEntity
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItemState
import ru.relabs.kurjer.domain.models.TaskState
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.SendQueryData

import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import java.util.*


/**
 * Created by ProOrange on 05.09.2018.
 */

object PersistenceHelper {

    //TODO: Add same functionality in refactored
    fun removeUnusedClosedTasks(db: AppDatabase) {
        //Remove all closed tasks, that haven't any report in query
        db.taskDao().allClosed.forEach {
            if (db.reportQueryDao().getByTaskId(it.id).isEmpty()) {
                db.taskDao().delete(it)
            }
        }
    }
}
