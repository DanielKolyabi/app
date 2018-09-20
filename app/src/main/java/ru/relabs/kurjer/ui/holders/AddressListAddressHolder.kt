package ru.relabs.kurjer.ui.holders

import android.graphics.Color
import android.view.View
import kotlinx.android.synthetic.main.item_addr_list_address.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.AddressModel
import ru.relabs.kurjer.ui.models.AddressListModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder


/**
 * Created by ProOrange on 11.08.2018.
 */
class AddressListAddressHolder(private val onMapClick: (address: AddressModel) -> Unit, private val showBypass: Boolean, itemView: View) : BaseViewHolder<AddressListModel>(itemView) {
    override fun onBindViewHolder(item: AddressListModel) {
        if (item !is AddressListModel.Address) return
        var address = item.taskItem.address.name
        if (showBypass) {
            address = "${item.taskItem.subarea}-${item.taskItem.bypass} $address"
        }
        itemView.address_text.text = address
        if (item.taskItem.state == TaskItemModel.CLOSED) {
            itemView.address_text.setTextColor(Color.parseColor("#CCCCCC"))
            itemView.map_icon.alpha = 0.4f
            itemView.map_icon.isClickable = false
        }else{
            itemView.address_text.setTextColor(itemView.resources.getColor(R.color.primary_text_default_material_light))
            itemView.map_icon.alpha = 1f
            itemView.map_icon.isClickable = true
        }

        itemView.map_icon.setOnClickListener {
            onMapClick(item.taskItem.address)
        }
    }
}