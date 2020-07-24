package ru.relabs.kurjer.uiOld.presenters

import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.SendQueryData
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.uiOld.fragments.TaskDetailsFragment
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.activity

class TaskDetailsPresenter(
    val fragment: TaskDetailsFragment,
    val database: AppDatabase,
    val dbRep: DatabaseRepository
) {
    fun onInfoClicked(item: TaskItemModel) {
        (fragment.context as? MainActivity)?.showTaskItemExplanation(item)
    }

    fun onExaminedClicked(task: TaskModel) {
        GlobalScope.launch(Dispatchers.Main) {

            withContext(Dispatchers.Default) {
                val taskEntity = database.taskDao().getById(task.id) ?: return@withContext

                taskEntity.state = TaskModel.EXAMINED
                database.taskDao().update(taskEntity)

                dbRep.putSendQuery(SendQueryData.TaskExamined(TaskId(taskEntity.id)))
            }
            fragment.activity()?.showTaskListScreen(false, fragment.posInList)
        }
    }

    fun onMapClicked(task: TaskModel) {
        val ctx = fragment.context ?: return
        val image = PathHelper.getTaskRasterizeMapFile(task)
        if (!image.exists()) {
            Toast.makeText(ctx, "Файл карты не найден.", Toast.LENGTH_SHORT).show()
            CustomLog.writeToFile("Для задания ${task.id} не удалось получить растровую карту. ${task.rastMapUrl}")
            return
        }

        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        val uri = FileProvider.getUriForFile(ctx, "com.relabs.kurjer.file_provider", image)
        intent.setDataAndType(uri, "image/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ContextCompat.startActivity(ctx, intent, null)
    }
}
