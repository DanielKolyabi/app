package ru.relabs.kurjer.ui.holders

import android.view.View
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.models.AddressListModel

class AddressListLoaderHolder(itemView: View) : BaseViewHolder<AddressListModel>(itemView) {
    override fun onBindViewHolder(item: AddressListModel) {
        if(item !is AddressListModel.Loader) return
    }
}
