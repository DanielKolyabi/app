package ru.relabs.kurjer.uiOld.holders

import android.view.View
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.uiOld.models.TaskListModel

class TaskListLoaderHolder(itemView: View) : BaseViewHolder<TaskListModel>(itemView) {
    override fun onBindViewHolder(item: TaskListModel) {
        if(item !is TaskListModel.Loader) return
    }
}
