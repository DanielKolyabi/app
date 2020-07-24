package ru.relabs.kurjer.uiOld.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.uiOld.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.uiOld.holders.TaskListTaskHolder
import ru.relabs.kurjer.uiOld.models.TaskListModel

/**
 * Created by ProOrange on 31.08.2018.
 */
class TaskListTaskDelegate(
        val onSelectedClicked: (position: Int) -> Unit,
        val onTaskClicked: (position: Int) -> Unit
) : IAdapterDelegate<TaskListModel> {

    override fun isForViewType(data: List<TaskListModel>, position: Int): Boolean {
        return data[position] is TaskListModel.Task
    }

    override fun onBindViewHolder(holder: BaseViewHolder<TaskListModel>, data: List<TaskListModel>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<TaskListModel> {
        return TaskListTaskHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_task_list_task, parent, false),
                {onSelectedClicked(it)},
                {onTaskClicked(it)}
        )
    }
}