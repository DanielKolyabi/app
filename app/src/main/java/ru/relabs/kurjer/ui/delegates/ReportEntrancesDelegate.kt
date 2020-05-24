package ru.relabs.kurjer.ui.delegates

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.ui.holders.ReportEntranceHolder
import ru.relabs.kurjer.ui.models.ReportEntrancesListModel

class ReportEntrancesDelegate(
        private val onSelectClicked: (type: Int, holder: RecyclerView.ViewHolder) -> Unit,
        private val onCoupleClicked: (entrancePosition: Int) -> Unit,
        private val onPhotoClicked: (entranceNumber: Int) -> Unit
) : IAdapterDelegate<ReportEntrancesListModel> {
    override fun isForViewType(data: List<ReportEntrancesListModel>, position: Int): Boolean {
        return data[position] is ReportEntrancesListModel.Entrance
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ReportEntrancesListModel>, data: List<ReportEntrancesListModel>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ReportEntrancesListModel> {
        return ReportEntranceHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_report_entrance, parent, false),
                onSelectClicked,
                onCoupleClicked,
                onPhotoClicked
        )
    }
}

