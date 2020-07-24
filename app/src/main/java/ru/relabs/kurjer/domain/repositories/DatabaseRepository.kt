package ru.relabs.kurjer.domain.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.entities.SendQueryItemEntity
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.utils.Either
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right

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
}

sealed class SendQueryData {
    data class PauseStart(val pauseType: PauseType, val startTime: Long) : SendQueryData()
    data class PauseStop(val pauseType: PauseType, val endTime: Long) : SendQueryData()

    data class TaskAccepted(val taskId: TaskId) : SendQueryData()
    data class TaskReceived(val taskId: TaskId) : SendQueryData()
    data class TaskExamined(val taskId: TaskId) : SendQueryData()
}

