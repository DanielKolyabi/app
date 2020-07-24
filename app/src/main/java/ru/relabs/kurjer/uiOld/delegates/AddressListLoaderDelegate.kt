package ru.relabs.kurjer.uiOld.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.uiOld.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.uiOld.models.AddressListModel
import ru.relabs.kurjer.uiOld.holders.AddressListLoaderHolder

class AddressListLoaderDelegate : IAdapterDelegate<AddressListModel> {

    override fun isForViewType(data: List<AddressListModel>, position: Int): Boolean {
        return data[position] is AddressListModel.Loader
    }

    override fun onBindViewHolder(holder: BaseViewHolder<AddressListModel>, data: List<AddressListModel>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<AddressListModel> {
        return AddressListLoaderHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_loading, parent, false))
    }
}
