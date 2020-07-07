package ru.relabs.kurjer.ui.presenters

import android.content.Intent
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.widget.Toast
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.*
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.persistence.entities.SendQueryItemEntity
import ru.relabs.kurjer.ui.fragments.TaskDetailsFragment
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.activity
import ru.relabs.kurjer.utils.application

class TaskDetailsPresenter(val fragment: TaskDetailsFragment) {
    fun onInfoClicked(item: TaskItemModel) {
        (fragment.context as? MainActivity)?.showTaskItemExplanation(item)
    }

    fun onExaminedClicked(task: TaskModel) {
        launch(UI) {
            val db = application().database

            withContext(CommonPool) {
                val taskEntity = db.taskDao().getById(task.id) ?: return@withContext

                taskEntity.state = TaskModel.EXAMINED
                db.taskDao().update(taskEntity)

                db.sendQueryDao().insert(
                        SendQueryItemEntity(0,
                                BuildConfig.API_URL + "/api/v1/tasks/${taskEntity.id}/examined?token=" + (application().user as UserModel.Authorized).token,
                                ""
                        )
                )
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
