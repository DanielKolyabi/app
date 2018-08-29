package ru.relabs.kurjer.ui.holders

import android.view.View
import ru.relabs.kurjer.models.DataModels
import ru.relabs.kurjer.models.DetailsTableHeader
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 29.08.2018.
 */
class DetailsTableInfoHolder(itemView: View) : BaseViewHolder<DataModels>(itemView) {
    override fun onBindViewHolder(item: DataModels) {
        if (item !is TaskModel) return
    }
}