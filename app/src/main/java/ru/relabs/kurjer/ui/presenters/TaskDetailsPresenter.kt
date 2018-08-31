package ru.relabs.kurjer.ui.presenters

import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.ui.fragments.TaskDetailsFragment

class TaskDetailsPresenter(val fragment: TaskDetailsFragment) {
    fun onInfoClicked(item: TaskItemModel): Unit {
        (fragment.context as? MainActivity)?.showTaskItemExplanation(item)
    }

}
