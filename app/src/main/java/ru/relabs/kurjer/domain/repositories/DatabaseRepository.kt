package ru.relabs.kurjer.domain.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.entities.SendQueryItemEntity
import ru.relabs.kurjer.domain.mappers.database.DatabaseAddressMapper
import ru.relabs.kurjer.domain.mappers.database.DatabaseEntranceDataMapper
import ru.relabs.kurjer.domain.mappers.database.DatabaseTaskItemMapper
import ru.relabs.kurjer.domain.mappers.database.DatabaseTaskMapper
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.utils.Either
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import ru.relabs.kurjer.utils.debug
import java.util.*

class DatabaseRepository(
    private val db: AppDatabase,
    private val authTokenStorage: AuthTokenStorage,
    private val baseUrl: String
) {

    suspend fun clearTasks() = withContext(Dispatchers.IO) {
        db.taskDao().all.forEach {
            removeTask(TaskId(it.id))
        }
    }

    suspend fun removeTask(taskId: TaskId) = withContext(Dispatchers.IO) {
        //Remove all taskItems
        db.taskItemDao().getAllForTask(taskId.id).forEach { taskItem ->
            db.taskItemResultsDao().getByTaskItemId(taskItem.id)?.let { taskItemResult ->
                db.entrancesDao().getByTaskItemResultId(taskItemResult.id).forEach { entrance ->
                    db.entrancesDao().delete(entrance)
                }
                db.taskItemResultsDao().delete(taskItemResult)
            }
            db.entranceDataDao().deleteAllForTaskItem(taskItem.id)
            db.taskItemDao().delete(taskItem)
        }
        //Remove task
        db.taskDao().getById(taskId.id)?.let {
            db.taskDao().delete(it)
        }

        //TODO: Remove Task Photo
        PathHelper.getTaskRasterizeMapFileById(taskId).delete()
    }

    suspend fun putSendQuery(sendData: SendQueryData): Either<Exception, SendQueryItemEntity> = withContext(Dispatchers.IO) {
        when (val r = mapSendDataToEntity(sendData)) {
            is Right -> {
                val id = db.sendQueryDao().insert(r.value)
                Right(r.value.copy(id = id.toInt()))
            }
            is Left -> r
        }
    }

    private fun mapSendDataToEntity(data: SendQueryData): Either<Exception, SendQueryItemEntity> {
        return when (data) {
            is SendQueryData.PauseStart -> getAuthorizedSendQuery(
                "$baseUrl/api/v1/pause/start",
                "type=${data.pauseType.toInt()}&time=${data.startTime}"
            )
            is SendQueryData.PauseStop -> getAuthorizedSendQuery(
                "$baseUrl/api/v1/pause/stop",
                "type=${data.pauseType.toInt()}&time=${data.endTime}"
            )
            is SendQueryData.TaskAccepted -> getAuthorizedSendQuery(
                "$baseUrl/api/v1/tasks/${data.taskId.id}/accepted"
            )
            is SendQueryData.TaskReceived -> getAuthorizedSendQuery(
                "$baseUrl/api/v1/tasks/${data.taskId.id}/received"
            )
            is SendQueryData.TaskExamined -> getAuthorizedSendQuery(
                "$baseUrl/api/v1/tasks/${data.taskId.id}/examined"
            )
        }
    }

    private fun getAuthorizedSendQuery(url: String, postData: String = ""): Either<Exception, SendQueryItemEntity> {
        val token = authTokenStorage.getToken() ?: throw RuntimeException("Empty auth token")
        return getSendQuery(url, postData, token)
    }

    private fun getSendQuery(url: String, postData: String, token: String?): Either<Exception, SendQueryItemEntity> = Either.of {
        SendQueryItemEntity(
            0,
            url + "?token=${token ?: ""}",
            postData
        )
    }

    suspend fun getTasks(): List<Task> = withContext(Dispatchers.IO) {
        db.taskDao().allOpened
            .map { DatabaseTaskMapper.fromEntity(it, db) }
            .filter { it.items.isNotEmpty() && Date() <= it.endTime }
    }

    fun closeTaskById(db: AppDatabase, taskId: Int) {
        //Remove all taskItems
        db.taskItemDao().getAllForTask(taskId).forEach { taskItem ->
            db.taskItemResultsDao().getByTaskItemId(taskItem.id)?.let { taskItemResult ->
                db.entrancesDao().getByTaskItemResultId(taskItemResult.id).forEach { entrance ->
                    db.entrancesDao().delete(entrance)
                }
                db.taskItemResultsDao().delete(taskItemResult)
            }
            db.entranceDataDao().deleteAllForTaskItem(taskItem.id)
            db.taskItemDao().delete(taskItem)
        }
        //Remove task
        db.taskDao().getById(taskId)?.let {
            db.taskDao().delete(it)
        }
        //Remove rast map
        //TODO: Remove rast map
    }

    suspend fun mergeTasks(tasks: List<Task>): Flow<MergeResult> = flow {
        val savedTasksIDs = db.taskDao().all.map { it.id }
        val newTasksIDs = tasks.map { it.id.id }

        //Задача отсутствует в ответе от сервера (удалено)
        db.taskDao().all.filter { it.id !in newTasksIDs }.forEach { task ->
            closeTaskById(db, task.id)
            emit(MergeResult.TaskRemoved(TaskId(task.id)))
        }

        //Задача не присутствует в сохранённых (новая)
        tasks.filter { it.id.id !in savedTasksIDs }.forEach { task ->
            if (task.state.state == TaskState.CANCELED) {
                debug("New task ${task.id} passed due 12 status")
                return@forEach
            }
            //Add task
            val newTaskId = db.taskDao().insert(DatabaseTaskMapper.toEntity(task))
            var openedTaskItems = 0
            for (item in task.items) {
                //Add address
                db.addressDao().insert(DatabaseAddressMapper.toEntity(item.address))
                //Add item
                val reportForThisTask = db.reportQueryDao().getByTaskItemId(item.id.id)
                if (item.state != TaskItemState.CLOSED && reportForThisTask == null) {
                    openedTaskItems++
                }
                if (reportForThisTask != null) {
                    db.taskItemDao().insert(DatabaseTaskItemMapper.toEntity(item.copy(state = TaskItemState.CLOSED)))
                } else {
                    db.taskItemDao().insert(DatabaseTaskItemMapper.toEntity(item))
                }
                db.entranceDataDao().insertAll(item.entrancesData.map { DatabaseEntranceDataMapper.toEntity(it, item.id) })
                debug("Add taskItem ID: ${item.id.id}")
            }
            if (openedTaskItems <= 0) {
                closeTaskById(db, newTaskId.toInt())
            } else {
                putSendQuery(SendQueryData.TaskReceived(task.id))
                emit(MergeResult.TaskCreated(task))
            }
        }

        //Задача есть и на сервере и на клиенте (мерж)
        /*
        Если она закрыта | выполнена на сервере - удалить с клиента
        Если итерация > сохранённой | состояние отличается от сохранённого и сохранённое != начато |
         */
        tasks.filter { it.id.id in savedTasksIDs }.forEach { task ->
            val savedTask = db.taskDao().getById(task.id.id) ?: return@forEach
            val savedTaskState = savedTask.state.toTaskState()
            if (task.state.state == TaskState.CANCELED) {
                if (savedTaskState == TaskState.STARTED) {
                    putSendQuery(SendQueryData.TaskAccepted(TaskId(savedTask.id)))
                } else {
                    emit(MergeResult.TaskRemoved(TaskId(savedTask.id)))
                    closeTaskById(db, savedTask.id)
                }
            } else if (task.state.state == TaskState.COMPLETED) {
                emit(MergeResult.TaskRemoved(TaskId(savedTask.id)))
                closeTaskById(db, savedTask.id)
                return@forEach
            } else if (
                (savedTask.iteration < task.iteration)
                || (task.state.state != savedTaskState && savedTaskState != TaskState.STARTED)
                || (task.endTime != savedTask.endTime || task.startTime != savedTask.startTime)
            ) {
                emit(MergeResult.TaskUpdated(task))

                db.taskDao().update(DatabaseTaskMapper.toEntity(task))

                val currentTasks = db.taskItemDao().getAllForTask(task.id.id).toMutableList()

                //Add new tasks and update old tasks
                task.items.forEach { newTaskItem ->
                    currentTasks.removeAll { oldTaskItem -> oldTaskItem.id == newTaskItem.id.id }

                    db.addressDao().insert(DatabaseAddressMapper.toEntity(newTaskItem.address))

                    val reportForThisTask = db.reportQueryDao().getByTaskItemId(newTaskItem.id.id)

                    db.taskItemDao().insert(
                        if (reportForThisTask != null) {
                            DatabaseTaskItemMapper.toEntity(newTaskItem.copy(state = TaskItemState.CLOSED))
                        } else {
                            DatabaseTaskItemMapper.toEntity(newTaskItem)
                        }
                    )

                    db.entranceDataDao()
                        .insertAll(newTaskItem.entrancesData.map { enData -> DatabaseEntranceDataMapper.toEntity(enData, newTaskItem.id) })
                }

                //Remove old taskItems
                currentTasks.forEach {
                    removeTaskItem(it.id)
                }
                //result.isTasksChanged = true
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun removeTaskItem(taskItemId: Int) {
        if (db.reportQueryDao().getByTaskItemId(taskItemId) == null) {
            db.photosDao().deleteByTaskItemId(taskItemId)
        }
        val result = db.taskItemResultsDao().getByTaskItemId(taskItemId)
        if (result != null) {
            db.entrancesDao().getByTaskItemResultId(result.id)
            db.taskItemResultsDao().delete(result)
        }
        db.entranceDataDao().deleteAllForTaskItem(taskItemId)
        db.taskItemDao().deleteById(taskItemId)
    }

    suspend fun getTask(id: TaskId): Task? = withContext(Dispatchers.IO) {
        db.taskDao().getById(id.id)?.let {
            DatabaseTaskMapper.fromEntity(it, db)
        }
    }

    suspend fun examineTask(task: Task): Task = withContext(Dispatchers.IO) {
        db.taskDao().getById(task.id.id)?.let {
            db.taskDao().update(it.copy(state = TaskState.EXAMINED.toInt()))

            putSendQuery(SendQueryData.TaskExamined(task.id))
        }

        task.copy(state = task.state.copy(state = TaskState.EXAMINED))
    }
}

sealed class SendQueryData {
    data class PauseStart(val pauseType: PauseType, val startTime: Long) : SendQueryData()
    data class PauseStop(val pauseType: PauseType, val endTime: Long) : SendQueryData()

    data class TaskAccepted(val taskId: TaskId) : SendQueryData()
    data class TaskReceived(val taskId: TaskId) : SendQueryData()
    data class TaskExamined(val taskId: TaskId) : SendQueryData()
}

sealed class MergeResult {
    data class TaskCreated(val task: Task) : MergeResult()
    data class TaskRemoved(val taskId: TaskId) : MergeResult()
    data class TaskUpdated(val task: Task) : MergeResult()
}

