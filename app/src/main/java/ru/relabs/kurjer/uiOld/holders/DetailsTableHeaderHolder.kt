package ru.relabs.kurjer.uiOld.holders

import android.view.View
import ru.relabs.kurjer.uiOld.models.DetailsListModel
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 29.08.2018.
 */
class DetailsTableHeaderHolder(itemView: View) : BaseViewHolder<DetailsListModel>(itemView) {
    override fun onBindViewHolder(item: DetailsListModel) {
        if (item !is DetailsListModel.DetailsTableHeader) return
    }
}