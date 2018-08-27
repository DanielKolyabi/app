package ru.relabs.kurjer.ui.holders

import android.view.View
import kotlinx.android.synthetic.main.item_address.view.*
import ru.relabs.kurjer.models.AddressElement
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 11.08.2018.
 */
class AddressHolder(itemView: View) : BaseViewHolder<AddressElement>(itemView) {
    override fun onBindViewHolder(item: AddressElement) {
        if (item !is AddressElement.AddressModel) return
        itemView.address_text.text = item.address
    }
}