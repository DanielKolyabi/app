package ru.relabs.kurjer.uiOld.holders

import android.view.View
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.uiOld.models.AddressListModel

class AddressListLoaderHolder(itemView: View) : BaseViewHolder<AddressListModel>(itemView) {
    override fun onBindViewHolder(item: AddressListModel) {
        if(item !is AddressListModel.Loader) return
    }
}
