package ru.relabs.kurjer.ui.presenters

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.MyApplication
import ru.relabs.kurjer.application
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.persistence.entities.SendQueryItemEntity
import ru.relabs.kurjer.ui.fragments.TaskDetailsFragment

class TaskDetailsPresenter(val fragment: TaskDetailsFragment) {
    fun onInfoClicked(item: TaskItemModel): Unit {
        (fragment.context as? MainActivity)?.showTaskItemExplanation(item)
    }

    fun onExaminedClicked(task: TaskModel) {
        launch(UI) {
            withContext(CommonPool) {
                val db = (fragment.activity!!.application as MyApplication).database
                val taskEntity = db.taskDao().getById(task.id)!!
                taskEntity.state = TaskModel.EXAMINED
                db.taskDao().update(taskEntity)

                db.sendQueryDao().insert(
                        SendQueryItemEntity(0,
                                BuildConfig.API_URL + "/api/v1/tasks/${taskEntity.id}/examined?token=" + (fragment.application()!!.user as UserModel.Authorized).token,
                                ""
                        )
                )
            }
            (fragment.context as MainActivity).showTaskListScreen()
        }
    }
}
