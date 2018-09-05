package ru.relabs.kurjer.ui.presenters

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.MyApplication
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.fragments.TaskDetailsFragment
import java.util.*

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
                taskEntity.updateTime = Date()
                db.taskDao().update(taskEntity)
            }
            (fragment.context as MainActivity).showTaskListScreen()
        }
    }
}
