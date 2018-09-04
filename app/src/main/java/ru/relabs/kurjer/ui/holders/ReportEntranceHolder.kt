package ru.relabs.kurjer.ui.holders

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Button
import kotlinx.android.synthetic.main.item_report_entrance.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.ui.models.ReportEntrancesListModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder

class ReportEntranceHolder(itemView: View, private val onSelectClicked: (type: Int, holder: RecyclerView.ViewHolder) -> Unit) : BaseViewHolder<ReportEntrancesListModel>(itemView) {
    override fun onBindViewHolder(item: ReportEntrancesListModel) {
        if(item !is ReportEntrancesListModel.Entrance) return
        itemView.entrance_title.text = "Подъезд ${item.entranceNumber}"
        with(itemView){
            setSelectButtonActive(euro_select, (item.selected and 0x0001) > 0)
            euro_select.setOnClickListener {
                onSelectClicked(0x0001, this@ReportEntranceHolder)
            }
            setSelectButtonActive(watch_select, (item.selected and 0x0010) > 0)
            watch_select.setOnClickListener {
                onSelectClicked(0x0010, this@ReportEntranceHolder)
            }
            setSelectButtonActive(pile_select, (item.selected and 0x0100) > 0)
            pile_select.setOnClickListener {
                onSelectClicked(0x0100, this@ReportEntranceHolder)
            }
            setSelectButtonActive(rejection_select, (item.selected and 0x1000) > 0)
            rejection_select.setOnClickListener {
                onSelectClicked(0x1000, this@ReportEntranceHolder)
            }
        }
    }

    fun setSelectButtonActive(button: Button, active: Boolean){
        if(active){
            button.setBackgroundResource(R.drawable.abc_btn_colored_material)
        }else{
            button.setBackgroundResource(R.drawable.abc_btn_default_mtrl_shape)
        }
    }
}
