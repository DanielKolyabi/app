package ru.relabs.kurjer.uiOld.holders

import android.graphics.Color
import android.view.View
import kotlinx.android.synthetic.main.item_addr_list_address.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.uiOld.models.AddressListModel


/**
 * Created by ProOrange on 11.08.2018.
 */
class AddressListAddressHolder(private val onMapClick: (List<TaskItemModel>) -> Unit, private val showBypass: Boolean, itemView: View) : BaseViewHolder<AddressListModel>(itemView) {
    override fun onBindViewHolder(item: AddressListModel) {
        if (item !is AddressListModel.Address) return
        var address = item.taskItems.first().address.name
        if (showBypass) {
            address = "${item.taskItems.first().subarea}-${item.taskItems.first().bypass} $address"
        }
        itemView.address_text.text = address
        val isAddressClosed = !item.taskItems.any { it.state != TaskItemModel.CLOSED }

        if (isAddressClosed) {
            itemView.address_text.setTextColor(Color.parseColor("#CCCCCC"))
            itemView.map_icon.alpha = 0.4f
            itemView.map_icon.isClickable = false
        } else {
            itemView.address_text.setTextColor(itemView.resources.getColor(R.color.primary_text_default_material_light))
            itemView.map_icon.alpha = 1f
            itemView.map_icon.isClickable = true
        }

        itemView.map_icon.setOnClickListener {
            onMapClick(item.taskItems)
        }
    }
}