package ru.relabs.kurjer.ui.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.AddressModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.ui.holders.AddressListAddressHolder
import ru.relabs.kurjer.ui.models.AddressListModel

/**
 * Created by ProOrange on 11.08.2018.
 */
class AddressListAddressDelegate(private val onMapClick: (address: AddressModel) -> Unit, private val showBypass: Boolean) : IAdapterDelegate<AddressListModel> {
    override fun isForViewType(data: List<AddressListModel>, position: Int): Boolean {
        return data[position] is AddressListModel.Address
    }

    override fun onBindViewHolder(holder: BaseViewHolder<AddressListModel>, data: List<AddressListModel>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<AddressListModel> {
        return AddressListAddressHolder(
                onMapClick,
                showBypass,
                LayoutInflater.from(parent.context).inflate(R.layout.item_addr_list_address, parent, false)
        )
    }
}