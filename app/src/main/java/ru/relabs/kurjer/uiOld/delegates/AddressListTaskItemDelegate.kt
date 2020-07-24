package ru.relabs.kurjer.uiOld.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.uiOld.models.AddressListModel
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.uiOld.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.uiOld.holders.AddressListTaskItemHolder

/**
 * Created by ProOrange on 11.08.2018.
 */
class AddressListTaskItemDelegate(
        private val onItemClicked: (task: AddressListModel.TaskItem) -> Unit,
        private val onItemMapClicked: (task: TaskModel) -> Unit
) : IAdapterDelegate<AddressListModel> {
    override fun isForViewType(data: List<AddressListModel>, position: Int): Boolean {
        return data[position] is AddressListModel.TaskItem
    }

    override fun onBindViewHolder(holder: BaseViewHolder<AddressListModel>, data: List<AddressListModel>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<AddressListModel> {
        return AddressListTaskItemHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_addr_list_task, parent, false),
                onItemClicked,
                onItemMapClicked
        )
    }
}