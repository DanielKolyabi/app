package ru.relabs.kurjer.domain.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.entities.EntranceWarningEntity
import ru.relabs.kurjer.data.database.entities.TaskItemEntity
import ru.relabs.kurjer.data.database.entities.TaskItemResultEntity
import ru.relabs.kurjer.data.database.entities.TaskItemResultEntranceEntity
import ru.relabs.kurjer.domain.mappers.ReportEntranceSelectionMapper
import ru.relabs.kurjer.domain.mappers.TaskItemEntranceResultMapper
import ru.relabs.kurjer.domain.mappers.database.DatabaseAddressMapper
import ru.relabs.kurjer.domain.mappers.database.DatabaseEntranceDataMapper
import ru.relabs.kurjer.domain.mappers.database.DatabaseTaskItemMapper
import ru.relabs.kurjer.domain.mappers.database.DatabaseTaskItemResultMapper
import ru.relabs.kurjer.domain.mappers.database.DatabaseTaskMapper
import ru.relabs.kurjer.domain.models.EntranceNumber
import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import ru.relabs.kurjer.domain.models.ReportEntranceSelection
import ru.relabs.kurjer.domain.models.StorageId
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemId
import ru.relabs.kurjer.domain.models.TaskItemResult
import ru.relabs.kurjer.domain.models.TaskItemResultId
import ru.relabs.kurjer.domain.models.TaskItemState
import ru.relabs.kurjer.domain.models.TaskState
import ru.relabs.kurjer.domain.models.address
import ru.relabs.kurjer.domain.models.id
import ru.relabs.kurjer.domain.models.needPhoto
import ru.relabs.kurjer.domain.models.state
import ru.relabs.kurjer.domain.models.toTaskState
import ru.relabs.kurjer.domain.providers.PathsProvider
import ru.relabs.kurjer.utils.debug
import java.util.Date

class TaskRepository(
    private val db: AppDatabase,
    private val photoRepository: PhotoRepository,
    private val queryRepository: QueryRepository,
    private val pathsProvider: PathsProvider
) {
    private val taskDao = db.taskDao()
    private val taskItemDao = db.taskItemDao()
    private val taskItemResultDao = db.taskItemResultsDao()
    private val entrancesDao = db.entrancesDao()
    private val entranceDataDao = db.entranceDataDao()
    private val addressDao = db.addressDao()
    private val entranceWarningDao = db.entranceWarningDao()

    suspend fun clearTasks() = withContext(Dispatchers.IO) {
        taskDao.all.map { it.id }
            .forEach { pathsProvider.getTaskRasterizeMapFileById(TaskId(it)).delete() }
        photoRepository.deleteAllTaskPhotos()
        db.clearAllTables()
    }

    suspend fun getTasks(): List<Task> = withContext(Dispatchers.IO) {
        taskDao.allOpened
            .map { DatabaseTaskMapper.fromEntity(it, db) }
            .filter { it.items.isNotEmpty() && Date() <= it.endTime }
    }

    suspend fun getTasksByIds(taskIds: List<TaskId>): List<Task> = withContext(Dispatchers.IO) {
        taskDao.getByIds(taskIds.map { it.id }).map { DatabaseTaskMapper.fromEntity(it, db) }
    }

    suspend fun closeTaskById(taskId: Int) = withContext(Dispatchers.IO) {
        //Remove all taskItems
        taskItemDao.getAllForTask(taskId).map { it.id }.let { taskItemIds ->
            with(taskItemResultDao) {
                getByIds(taskItemIds).map { it.id }.let { taskItemResultIds ->
                    entrancesDao.deleteByTaskItemResultIds(taskItemResultIds)
                }
                deleteByTaskItemIds(taskItemIds)
            }
        }
        taskItemDao.deleteByTaskId(taskId)
        taskDao.deleteById(taskId)
        //Remove rast map
        pathsProvider.getTaskRasterizeMapFileById(TaskId(taskId)).delete()
        pathsProvider.getEditionPhotoFileById(TaskId(taskId)).delete()
    }

    suspend fun mergeTasks(tasks: List<Task>): Flow<MergeResult> = flow {
        val savedTasksIDs = taskDao.all.map { it.id }
        val newTasksIDs = tasks.map { it.id.id }


        //Задача отсутствует в ответе от сервера (удалено)
        taskDao.all.filter { it.id !in newTasksIDs }.forEach { task ->
            closeTaskById(task.id)
            emit(MergeResult.TaskRemoved(TaskId(task.id)))
        }

        //Задача не присутствует в сохранённых (новая)
        tasks.filter { it.id.id !in savedTasksIDs }.forEach { task ->
            if (task.state.state == TaskState.CANCELED) {
                debug("New task ${task.id} passed due 12 status")
                return@forEach
            }
            //Add task
            val newTaskId = taskDao.insert(DatabaseTaskMapper.toEntity(task))
            var openedTaskItems = 0
            for (item in task.items) {
                //Add address
                addressDao.insert(DatabaseAddressMapper.toEntity(item.address))
                //Add item
                val reportForThisTask = db.reportQueryDao().getByTaskItemId(item.id.id)
                if (item.state != TaskItemState.CLOSED && reportForThisTask == null) {
                    openedTaskItems++
                }
                if (reportForThisTask != null) {
                    db.taskItemDao().insert(
                        DatabaseTaskItemMapper.toEntity(item)
                            .copy(state = TaskItemEntity.STATE_CLOSED)
                    )
                } else {
                    db.taskItemDao().insert(DatabaseTaskItemMapper.toEntity(item))
                }
                if (item is TaskItem.Common) {
                    db.entranceDataDao().insertAll(item.entrancesData.map {
                        DatabaseEntranceDataMapper.toEntity(
                            it,
                            item.id
                        )
                    })
                }
                debug("Add taskItem ID: ${item.id.id}")
            }
            if (openedTaskItems <= 0) {
                closeTaskById(newTaskId.toInt())
            } else {
                queryRepository.putSendQuery(SendQueryData.TaskReceived(task.id))
                emit(MergeResult.TaskCreated(task))
            }
        }

        //Задача есть и на сервере и на клиенте (мерж)
        /*
        Если она закрыта | выполнена на сервере - удалить с клиента
        Если итерация > сохранённой | состояние отличается от сохранённого и сохранённое != начато |
         */
        tasks.filter { it.id.id in savedTasksIDs }.forEach { task ->
            val savedTask = taskDao.getById(task.id.id) ?: return@forEach
            val savedTaskState = savedTask.state.toTaskState()
            if (task.state.state == TaskState.CANCELED) {
                if (savedTaskState == TaskState.STARTED) {
                    queryRepository.putSendQuery(SendQueryData.TaskAccepted(TaskId(savedTask.id)))
                } else {
                    emit(MergeResult.TaskRemoved(TaskId(savedTask.id)))
                    closeTaskById(savedTask.id)
                }
            } else if (task.state.state == TaskState.COMPLETED) {
                emit(MergeResult.TaskRemoved(TaskId(savedTask.id)))
                closeTaskById(savedTask.id)
                return@forEach
            } else if (
                (savedTask.iteration < task.iteration)
                || (task.state.state != savedTaskState && savedTaskState != TaskState.STARTED)
                || (task.endTime != savedTask.endTime || task.startTime != savedTask.startTime)
            ) {
                emit(MergeResult.TaskUpdated(task))

                entranceWarningDao.deleteByTaskId(task.id.id)

                queryRepository.putSendQuery(SendQueryData.TaskReceived(TaskId(savedTask.id)))

                taskDao.update(DatabaseTaskMapper.toEntity(task))

                val currentTasks = db.taskItemDao().getAllForTask(task.id.id).toMutableList()

                //Add new tasks and update old tasks
                task.items.forEach { newTaskItem ->
                    currentTasks.removeAll { oldTaskItem -> oldTaskItem.id == newTaskItem.id.id }

                    db.addressDao().insert(DatabaseAddressMapper.toEntity(newTaskItem.address))

                    val reportForThisTask = db.reportQueryDao().getByTaskItemId(newTaskItem.id.id)

                    db.taskItemDao().insert(
                        if (reportForThisTask != null) {
                            DatabaseTaskItemMapper.toEntity(newTaskItem)
                                .copy(state = TaskItemEntity.STATE_CLOSED)
                        } else {
                            DatabaseTaskItemMapper.toEntity(newTaskItem)
                        }
                    )

                    if (newTaskItem is TaskItem.Common) {
                        db.entranceDataDao()
                            .insertAll(newTaskItem.entrancesData.map { enData ->
                                DatabaseEntranceDataMapper.toEntity(
                                    enData,
                                    newTaskItem.id
                                )
                            })
                    }
                }

                //Remove old taskItems
                currentTasks.forEach {
                    removeTaskItem(it.id)
                }
                //result.isTasksChanged = true
            } else if (savedTask.listSort != task.listSort) {
                emit(MergeResult.TaskUpdated(task))
                taskDao.update(savedTask.copy(listSort = task.listSort))
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun removeTaskItem(taskItemId: Int) {
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
        taskDao.getById(id.id)?.let {
            DatabaseTaskMapper.fromEntity(it, db)
        }
    }

    suspend fun examineTask(task: Task): Task = withContext(Dispatchers.IO) {
        if (task.state.state != TaskState.CREATED) {
            return@withContext task
        }

        taskDao.getById(task.id.id)?.let {
            taskDao.update(it.copy(state = TaskState.EXAMINED.toInt(), byOtherUser = false))

            queryRepository.putSendQuery(SendQueryData.TaskExamined(task.id))
        }

        task.copy(state = Task.State(TaskState.EXAMINED, false))
    }

    suspend fun getTaskItem(id: TaskItemId): TaskItem? = withContext(Dispatchers.IO) {
        db.taskItemDao().getById(id.id)?.let { DatabaseTaskItemMapper.fromEntity(it, db) }
    }

    suspend fun getTaskItemResult(taskItem: TaskItem): TaskItemResult? =
        withContext(Dispatchers.IO) {
            db.taskItemResultsDao().getByTaskItemId(taskItem.id.id)?.let {
                DatabaseTaskItemResultMapper.fromEntity(db, it)
            }
        }

    suspend fun updateTaskItemResult(updatedReport: TaskItemResult): TaskItemResult =
        withContext(Dispatchers.IO) {
            val newId = db.taskItemResultsDao()
                .insert(DatabaseTaskItemResultMapper.fromModel(updatedReport))
            db.entrancesDao().insertAll(
                updatedReport.entrances.map {
                    TaskItemEntranceResultMapper.fromModel(
                        if (it.taskItemResultId.id == 0) {
                            it.copy(taskItemResultId = TaskItemResultId(newId.toInt()))
                        } else {
                            it
                        }
                    )
                }
            )

            updatedReport.copy(id = TaskItemResultId(newId.toInt()))
        }

    suspend fun createOrUpdateTaskItemEntranceResult(
        entrance: EntranceNumber,
        taskItem: TaskItem,
        selectionUpdater: (TaskItemResultEntranceEntity) -> TaskItemResultEntranceEntity
    ): TaskItemResult? = withContext(Dispatchers.IO) {
        val isPhotoRequired = when (val ti = taskItem) {
            is TaskItem.Common -> ti.entrancesData.firstOrNull { it.number == entrance }?.photoRequired
                ?: false

            is TaskItem.Firm -> ti.needPhoto
        }
        val taskItemResult = db.taskItemResultsDao().getByTaskItemId(taskItem.id.id)
            ?: createEmptyTaskItemResult(taskItem.id, taskItem.needPhoto)
        val entranceResult = db.entrancesDao().getByTaskItemResultId(taskItemResult.id)
            .firstOrNull { it.entrance == entrance.number }
            ?: createEmptyTaskItemEntranceResult(
                TaskItemResultId(taskItemResult.id),
                entrance,
                isPhotoRequired
            )

        db.entrancesDao().update(selectionUpdater(entranceResult))

        getTaskItemResult(taskItem)
    }

    suspend fun createOrUpdateTaskItemEntranceResultSelection(
        entrance: EntranceNumber,
        taskItem: TaskItem,
        selectionUpdater: (ReportEntranceSelection) -> ReportEntranceSelection
    ): TaskItemResult? = withContext(Dispatchers.IO) {
        createOrUpdateTaskItemEntranceResult(entrance, taskItem) {
            it.copy(
                state = ReportEntranceSelectionMapper.toBits(
                    selectionUpdater(ReportEntranceSelectionMapper.fromBits(it.state))
                )
            )
        }

        getTaskItemResult(taskItem)
    }

    suspend fun createOrUpdateTaskItemEntranceResultSelection(
        entrance: EntranceNumber,
        taskItem: TaskItem,
        selection: ReportEntranceSelection
    ): TaskItemResult? = withContext(Dispatchers.IO) {
        createOrUpdateTaskItemEntranceResultSelection(entrance, taskItem) { selection }
    }

    private suspend fun createEmptyTaskItemEntranceResult(
        taskItemResultId: TaskItemResultId,
        entranceNumber: EntranceNumber,
        isPhotoRequired: Boolean
    ): TaskItemResultEntranceEntity = withContext(Dispatchers.IO) {
        val empty =
            TaskItemResultEntranceEntity.empty(taskItemResultId, entranceNumber, isPhotoRequired)
        val id = db.entrancesDao().insert(empty)

        empty.copy(id = id.toInt())
    }

    private suspend fun createEmptyTaskItemResult(
        taskItemId: TaskItemId,
        isPhotoRequired: Boolean
    ): TaskItemResultEntity =
        withContext(Dispatchers.IO) {
            val empty = TaskItemResultEntity.empty(taskItemId, isPhotoRequired)
            val id = db.taskItemResultsDao().insert(empty)

            empty.copy(id = id.toInt())
        }

    suspend fun closeTaskItem(taskItemId: TaskItemId, fromRemote: Boolean = false) =
        withContext(Dispatchers.IO) {
            val taskItemEntity = taskItemDao.getById(taskItemId.id)
            val parentTaskId = taskItemEntity?.taskId

            taskItemEntity
                ?.copy(state = TaskItemEntity.STATE_CLOSED, closeTime = Date())
                ?.let { taskItemDao.update(it) }

            parentTaskId?.let { taskId ->
                taskDao.getById(taskId)?.let { parentTask ->
                    val state = parentTask.state.toTaskState()
                    if (state == TaskState.EXAMINED || state == TaskState.CREATED) {
                        if (!fromRemote) {
                            queryRepository.putSendQuery(SendQueryData.TaskAccepted(TaskId(parentTask.id)))
                        }
                        taskDao.update(parentTask.copy(state = TaskState.STARTED.toInt()))
                    }
                }
            }
        }

    suspend fun isTaskCloseRequired(taskId: TaskId): Boolean = withContext(Dispatchers.IO) {
        taskItemDao.getAllForTask(taskId.id).none { it.state == TaskItemEntity.STATE_CREATED }
    }

    suspend fun isOpenedTasksExists(): Boolean = withContext(Dispatchers.IO) {
        taskDao.allOpened.isNotEmpty()
    }

    suspend fun getTaskEntityIds() = withContext(Dispatchers.IO) {
        taskDao.all.map { it.id }
    }

    suspend fun getTaskEntityById(id: Int) = withContext(Dispatchers.IO) {
        taskDao.getById(id)
    }

    suspend fun getTasksByStorageId(storageIds: List<StorageId>): List<Task> =
        withContext(Dispatchers.IO) {
            taskDao.getTasksByStorageId(storageIds.map { it.id }).map {
                DatabaseTaskMapper.fromEntity(it, db)
            }
        }

    suspend fun updateTask(newTask: Task) {
        withContext(Dispatchers.IO) {
            taskDao.update(DatabaseTaskMapper.toEntity(newTask))
        }
    }

    suspend fun updateTaskItem(taskItem: TaskItem) {
        withContext(Dispatchers.IO) {
            taskItemDao.update(DatabaseTaskItemMapper.toEntity(taskItem))
        }
    }

    suspend fun createEmptyTaskResult(taskItem: TaskItem): TaskItemResult {
        val result = TaskItemResult(
            id = TaskItemResultId(0),
            taskItemId = taskItem.id,
            closeTime = null,
            description = "",
            entrances = emptyList(),
            gps = GPSCoordinatesModel(0.0, 0.0, Date()),
            isPhotoRequired = taskItem.needPhoto
        )
        return updateTaskItemResult(result)
    }

    fun watchTasks(taskIds: List<TaskId>): Flow<List<Task>> =
        taskDao.watchByIds(taskIds.map { it.id }).map { it.map { entity -> DatabaseTaskMapper.fromEntity(entity, db) } }

    suspend fun getWarning(
        taskItemId: TaskItemId,
        entranceNumber: EntranceNumber
    ) = withContext(Dispatchers.IO) {
        entranceWarningDao.getWarning(entranceNumber.number, taskItemId.id)
    }

    suspend fun createWarning(entranceNumber: EntranceNumber, taskItemId: TaskItemId, taskId: TaskId) = withContext(Dispatchers.IO) {
        entranceWarningDao.insert(EntranceWarningEntity(0, entranceNumber.number, taskId.id, taskItemId.id))
    }



}


sealed class MergeResult {
    data class TaskCreated(val task: Task) : MergeResult()
    data class TaskRemoved(val taskId: TaskId) : MergeResult()
    data class TaskUpdated(val task: Task) : MergeResult()
}


