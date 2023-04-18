package ru.relabs.kurjer.domain.useCases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskState
import ru.relabs.kurjer.domain.repositories.TaskRepository
import ru.relabs.kurjer.utils.CustomLog

class TaskUseCase(
    private val taskRepository: TaskRepository,
) {
    suspend fun getTasksByIds(taskIds: List<TaskId>): List<Task> {
        return taskRepository.getTasksByIds(taskIds)
    }

    suspend fun isOpenedTasksExists() = taskRepository.isOpenedTasksExists()

    suspend fun isMergeNeeded(newTasks: List<Task>): Boolean = withContext(Dispatchers.IO) {
        val savedTasksIDs = taskRepository.getTaskEntityIds()

        if (newTasks.any { it.id.id !in savedTasksIDs }) {
            CustomLog.writeToFile("UPDATE (Merge): ${newTasks.firstOrNull { it.id.id !in savedTasksIDs }?.id?.id} is new")
            return@withContext true
        }

        newTasks.filter { it.id.id in savedTasksIDs }.forEach { task ->
            val savedTask = taskRepository.getTaskEntityById(task.id.id)!!
            if (task.state.state == TaskState.CANCELED) {
                CustomLog.writeToFile("UPDATE (Merge): ${task.id.id} remote task is closed")
                return@withContext true
            } else if (task.state.state == TaskState.COMPLETED) {
                CustomLog.writeToFile("UPDATE (Merge): ${task.id.id} remote task is completed")
                return@withContext true
            } else if (
                (savedTask.iteration < task.iteration)
                || (task.state.state.toInt() != savedTask.state && savedTask.state != TaskState.STARTED.toInt())
                || (task.endTime != savedTask.endTime || task.startTime != savedTask.startTime && savedTask.state != TaskState.STARTED.toInt())
            ) {
                CustomLog.writeToFile("UPDATE (Merge): ${task.id.id} time/iteration/state updated but task not started")
                CustomLog.writeToFile("UPDATE (Merge): ${task.id.id} iter: ${savedTask.iteration < task.iteration} savedIter: ${savedTask.iteration}; newTaskIter: ${task.iteration}")
                CustomLog.writeToFile("UPDATE (Merge): ${task.id.id} state: ${savedTask.state != task.state.state.toInt()} savedState: ${savedTask.state}; newTaskState: ${task.state.state.toInt()}")
                CustomLog.writeToFile("UPDATE (Merge): ${task.id.id} time: ${task.endTime != savedTask.endTime || task.startTime != savedTask.startTime} savedTime: start: ${savedTask.startTime.time}; end: ${savedTask.endTime.time}; newTaskTime: start: ${task.startTime.time} end: ${task.endTime.time}")
                return@withContext true
            }
        }
        return@withContext false
    }
}