package ru.relabs.kurjer.ui.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.ui.models.AddressListModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.ui.holders.AddressListSortingHolder

/**
 * Created by ProOrange on 29.08.2018.
 */
class AddressListSortingDelegate(val onSortingChanged: (sortingMethod: Int) -> Unit) : IAdapterDelegate<AddressListModel> {
    override fun isForViewType(data: List<AddressListModel>, position: Int): Boolean {
        return data[position] is AddressListModel.SortingItem
    }

    override fun onBindViewHolder(holder: BaseViewHolder<AddressListModel>, data: List<AddressListModel>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<AddressListModel> {
        return AddressListSortingHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_addr_list_sorting, parent, false),
                { onSortingChanged(it) }
        )
    }
}