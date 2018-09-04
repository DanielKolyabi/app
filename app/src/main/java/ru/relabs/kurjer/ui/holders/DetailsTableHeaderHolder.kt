package ru.relabs.kurjer.ui.holders

import android.view.View
import ru.relabs.kurjer.ui.models.DetailsListModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 29.08.2018.
 */
class DetailsTableHeaderHolder(itemView: View) : BaseViewHolder<DetailsListModel>(itemView) {
    override fun onBindViewHolder(item: DetailsListModel) {
        if (item !is DetailsListModel.DetailsTableHeader) return
    }
}