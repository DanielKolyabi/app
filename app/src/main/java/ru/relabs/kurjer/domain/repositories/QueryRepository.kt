package ru.relabs.kurjer.domain.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.entities.ReportQueryItemEntity
import ru.relabs.kurjer.data.database.entities.SendQueryItemEntity
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.providers.PathsProvider
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.Either
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import java.util.UUID

class QueryRepository(
    private val db: AppDatabase,
    private val authTokenStorage: AuthTokenStorage,
    private val baseUrl: String,
    private val pathsProvider: PathsProvider
) {

    suspend fun removeReport(report: ReportQueryItemEntity) {
        db.reportQueryDao().delete(report)
        if (report.removeAfterSend) {
            db.photosDao().getByTaskItemId(report.taskItemId).forEach {
                //Delete photo
                CustomLog.writeToFile("Remove photos due to report removal tii=${report.taskItemId} ti=${it.UUID}")
                val file = pathsProvider.getTaskItemPhotoFileByID(
                    report.taskItemId,
                    UUID.fromString(it.UUID)
                )
                file.delete()
                db.photosDao().delete(it)
            }
            pathsProvider.getTaskItemPhotoFolderById(report.taskItemId).delete()
        }
    }

    suspend fun getNextSendQuery(): SendQueryItemEntity? = withContext(Dispatchers.IO) {
        db.sendQueryDao().all.firstOrNull()
    }

    suspend fun getNextReportQuery(): ReportQueryItemEntity? = withContext(Dispatchers.IO) {
        db.reportQueryDao().all.firstOrNull()
    }

    suspend fun getQueryItemsCount(): Int = withContext(Dispatchers.IO) {
        db.reportQueryDao().all.size + db.sendQueryDao().all.size + db.storageReportRequestDao().all.size
    }

    suspend fun removeSendQuery(sendQuery: SendQueryItemEntity) = withContext(Dispatchers.IO) {
        db.sendQueryDao().delete(sendQuery)
    }

    suspend fun createTaskItemReport(reportItem: ReportQueryItemEntity) =
        withContext(Dispatchers.IO) {
            db.reportQueryDao().insert(reportItem)
        }

    suspend fun putSendQuery(sendData: SendQueryData): Either<Exception, SendQueryItemEntity> =
        withContext(Dispatchers.IO) {
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

    private fun getAuthorizedSendQuery(
        url: String,
        postData: String = ""
    ): Either<Exception, SendQueryItemEntity> {
        val token = authTokenStorage.getToken() ?: throw RuntimeException("Empty auth token")
        return getSendQuery(url, postData, token)
    }

    private fun getSendQuery(
        url: String,
        postData: String,
        token: String?
    ): Either<Exception, SendQueryItemEntity> = Either.of {
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