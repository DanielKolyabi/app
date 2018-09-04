package ru.relabs.kurjer.ui.holders

import android.view.View
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.models.TaskListModel

class TaskListLoaderHolder(itemView: View) : BaseViewHolder<TaskListModel>(itemView) {
    override fun onBindViewHolder(item: TaskListModel) {
        if(item !is TaskListModel.Loader) return
    }
}
