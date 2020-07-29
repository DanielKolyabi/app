package ru.relabs.kurjer.uiOld.presenters

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskState
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.SendQueryData
import ru.relabs.kurjer.uiOld.fragments.TaskDetailsOldFragment
import ru.relabs.kurjer.utils.activity

class TaskDetailsPresenter(
    val fragment: TaskDetailsOldFragment,
    val database: AppDatabase,
    val dbRep: DatabaseRepository
) {
    fun onInfoClicked(item: TaskItem) {
        (fragment.context as? MainActivity)?.showTaskItemExplanation(item)
    }

    fun onExaminedClicked(task: Task) {
        GlobalScope.launch(Dispatchers.Main) {

            withContext(Dispatchers.Default) {
                val taskEntity = database.taskDao().getById(task.id.id) ?: return@withContext

                taskEntity.state = TaskState.EXAMINED.toInt()
                database.taskDao().update(taskEntity)

                dbRep.putSendQuery(SendQueryData.TaskExamined(TaskId(taskEntity.id)))
            }
            fragment.activity()?.showTaskListScreen(false, fragment.posInList)
        }
    }

    fun onMapClicked(task: Task) {
        val ctx = fragment.context ?: return
//        val image = PathHelper.getTaskRasterizeMapFile(task)
//        if (!image.exists()) {
//            Toast.makeText(ctx, "Файл карты не найден.", Toast.LENGTH_SHORT).show()
//            CustomLog.writeToFile("Для задания ${task.id} не удалось получить растровую карту. ${task.rastMapUrl}")
//            return
//        }
//
//        val intent = Intent()
//        intent.action = Intent.ACTION_VIEW
//        val uri = FileProvider.getUriForFile(ctx, "com.relabs.kurjer.file_provider", image)
//        intent.setDataAndType(uri, "image/*")
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        ContextCompat.startActivity(ctx, intent, null)
    }
}
