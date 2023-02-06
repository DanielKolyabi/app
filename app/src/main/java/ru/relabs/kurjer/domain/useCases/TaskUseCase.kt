package ru.relabs.kurjer.domain.useCases

import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.repositories.DatabaseRepository

class TaskUseCase(
    private val databaseRepository: DatabaseRepository,
) {
    suspend fun getTasksByIds(taskIds: List<TaskId>): List<Task> {
        return databaseRepository.getTasksByIds(taskIds)
    }
}