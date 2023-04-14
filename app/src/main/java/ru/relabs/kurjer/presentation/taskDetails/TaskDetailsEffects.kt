package ru.relabs.kurjer.presentation.taskDetails

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.TaskItem

import ru.relabs.kurjer.presentation.RootScreen

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */
object TaskDetailsEffects {

    fun effectNavigateBack(): TaskDetailsEffect = { c, s ->
        withContext(Dispatchers.Main) {
            c.router.exit()
        }
    }

    fun effectNavigateTaskItemDetails(taskItem: TaskItem): TaskDetailsEffect = { c, s ->
        withContext(Dispatchers.Main) {
            c.router.navigateTo(RootScreen.taskItemDetails(taskItem))
        }
    }

    fun effectExamine(): TaskDetailsEffect = { c, s ->
        messages.send(TaskDetailsMessages.msgAddLoaders(1))
        when (val t = s.task) {
            null -> c.showFatalError("tde:101")
            else -> {
                c.onExamine(c.taskRepository.examineTask(t))
                val editionPhotoPath = c.pathsProvider.getEditionPhotoFile(t).takeIf { it.exists() }?.path
                withContext(Dispatchers.Main) {
                    when (editionPhotoPath) {
                        null -> c.router.exit()
                        else -> c.router.replaceScreen(RootScreen.imagePreview(listOf(editionPhotoPath)))
                    }
                }
            }
        }
        messages.send(TaskDetailsMessages.msgAddLoaders(-1))
    }

    fun effectOpenMap(): TaskDetailsEffect = { c, s ->
        messages.send(TaskDetailsMessages.msgAddLoaders(1))
        when (val t = s.task) {
            null -> c.showFatalError("tde:102")
            else -> {
                val file = c.pathsProvider.getTaskRasterizeMapFile(t)
                when (file.exists()) {
                    true -> c.showImagePreview(file)
                    else -> c.showSnackbar(R.string.image_map_not_found)
                }
            }
        }
        messages.send(TaskDetailsMessages.msgAddLoaders(-1))
    }
}