package ru.relabs.kurjer.ui.holders

import android.view.View
import kotlinx.android.synthetic.main.item_address.view.*
import ru.relabs.kurjer.models.AddressElement
import ru.relabs.kurjer.models.DataModels
import ru.relabs.kurjer.models.DetailsTableHeader
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 29.08.2018.
 */
class DetailsTableHeaderHolder(itemView: View) : BaseViewHolder<DataModels>(itemView) {
    override fun onBindViewHolder(item: DataModels) {
        if (item !is DetailsTableHeader) return
    }
}