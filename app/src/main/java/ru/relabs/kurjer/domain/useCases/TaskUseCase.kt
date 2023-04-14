package ru.relabs.kurjer.domain.useCases

import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.repositories.TaskRepository

class TaskUseCase(
    private val taskRepository: TaskRepository,
) {
    suspend fun getTasksByIds(taskIds: List<TaskId>): List<Task> {
        return taskRepository.getTasksByIds(taskIds)
    }
}