package ru.relabs.kurjer.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.ui.holders.TaskViewHolder

/**
 * Created by ProOrange on 29.08.2018.
 */
class TaskListAdapter(
        val onTaskSelected: (position: Int) -> Unit,
        val onTaskClicked: (position: Int) -> Unit
)
    : RecyclerView.Adapter<TaskViewHolder>() {

    val data = mutableListOf<TaskModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        return TaskViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_task_list_task, parent, false),
                onTaskSelected,
                onTaskClicked
        )
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(data[position])
    }
}