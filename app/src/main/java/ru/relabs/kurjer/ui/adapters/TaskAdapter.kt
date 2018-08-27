package ru.relabs.kurjer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.AddressElement
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.ui.holders.TaskHolder

/**
 * Created by ProOrange on 11.08.2018.
 */
class TaskAdapter : IAdapterDelegate<AddressElement> {
    override fun isForViewType(data: List<AddressElement>, position: Int): Boolean {
        return data[position] is AddressElement.TaskModel
    }

    override fun onBindViewHolder(holder: BaseViewHolder<AddressElement>, data: List<AddressElement>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<AddressElement> {
        return TaskHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false))
    }
}