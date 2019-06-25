package ru.relabs.kurjer.ui.holders

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Button
import kotlinx.android.synthetic.main.item_report_entrance.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.models.ReportEntrancesListModel

class ReportEntranceHolder(
        itemView: View, private val onSelectClicked: (type: Int, holder: RecyclerView.ViewHolder) -> Unit,
        private val onCoupleClicked: (entrancePosition: Int) -> Unit
) : BaseViewHolder<ReportEntrancesListModel>(itemView) {
    override fun onBindViewHolder(item: ReportEntrancesListModel) {
        if (item !is ReportEntrancesListModel.Entrance) return
        val entranceData = item.taskItem.entrancesData.firstOrNull { it.number == item.entranceNumber }
        val apartmentsCount = entranceData?.apartmentsCount ?: "?"
        itemView.entrance_title.text = "${item.entranceNumber}п-${apartmentsCount}"
        with(itemView) {
            setSelectButtonActive(euro_select, (item.selected and 0x0001) > 0, entranceData?.isEuroBoxes == true)
            euro_select.text = if ((item.selected and 0x0001) > 0) "Евро" else "Щелев"
            euro_select.setOnClickListener {
                onSelectClicked(0x0001, this@ReportEntranceHolder)
            }
            setSelectButtonActive(watch_select, (item.selected and 0x0010) > 0, entranceData?.hasLookout == true)
            watch_select.setOnClickListener {
                onSelectClicked(0x0010, this@ReportEntranceHolder)
            }
            setSelectButtonActive(pile_select, (item.selected and 0x0100) > 0, entranceData?.isStacked == true)
            pile_select.setOnClickListener {
                onSelectClicked(0x0100, this@ReportEntranceHolder)
            }
            setSelectButtonActive(rejection_select, (item.selected and 0x1000) > 0, entranceData?.isRefused == true)
            rejection_select.setOnClickListener {
                onSelectClicked(0x1000, this@ReportEntranceHolder)
            }
        }

        itemView.entrance_title.setOnClickListener {
            onCoupleClicked(this.adapterPosition)
        }

        if (!item.coupleEnabled) {
            itemView.entrance_title.setBackgroundResource(R.drawable.bg_entrance_couple_enabled)
        } else {
            itemView.entrance_title.setBackgroundColor(Color.parseColor("#000000ff"))
        }
    }

    fun setSelectButtonActive(button: Button, active: Boolean, hasDefaultValue: Boolean = false) {
        if (active) {
            button.setBackgroundResource(R.drawable.abc_btn_colored_material)
        } else {
            button.setBackgroundResource(R.drawable.abc_btn_default_mtrl_shape)
        }
        if (!hasDefaultValue || active) {
            button.backgroundTintList = null
        } else if (!active) {
            button.backgroundTintList = button.context.resources.getColorStateList(R.color.button_default_color_list)
        }
    }
}
