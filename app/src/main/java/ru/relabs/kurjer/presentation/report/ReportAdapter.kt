package ru.relabs.kurjer.presentation.report

import android.graphics.Color
import android.widget.Button
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.holder_report_entrance.view.*
import kotlinx.android.synthetic.main.holder_report_photo.view.*
import kotlinx.android.synthetic.main.holder_report_task.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.presentation.base.recycler.IAdapterDelegate
import ru.relabs.kurjer.presentation.base.recycler.delegateDefine
import ru.relabs.kurjer.presentation.base.recycler.holderDefine
import kotlinx.android.synthetic.main.holder_report_photo.view.iv_photo as photo
import kotlinx.android.synthetic.main.holder_report_photo_single.view.iv_photo as singlePhoto

object ReportAdapter {

    fun photoSingle(onPhotoClicked: () -> Unit): IAdapterDelegate<ReportPhotoItem> = delegateDefine(
        { it is ReportPhotoItem.Single },
        { p ->
            holderDefine(p, R.layout.holder_report_photo_single, { it as ReportPhotoItem.Single }) { (required, hasPhoto) ->
                itemView.setOnClickListener {
                    onPhotoClicked()
                }

                val photoImgRes = if (hasPhoto) {
                    R.drawable.ic_house_photo_done
                } else {
                    when (required) {
                        true -> R.drawable.ic_house_photo_req
                        else -> R.drawable.ic_house_photo
                    }
                }
                itemView.singlePhoto.setImageResource(photoImgRes)
            }
        }
    )

    fun photo(onRemoveClicked: (TaskItemPhoto) -> Unit): IAdapterDelegate<ReportPhotoItem> = delegateDefine(
        { it is ReportPhotoItem.Photo },
        { p ->
            holderDefine(p, R.layout.holder_report_photo, { it as ReportPhotoItem.Photo }) { (photo, uri) ->
                itemView.iv_remove.setOnClickListener {
                    onRemoveClicked(photo)
                }

                Glide.with(itemView)
                    .load(uri)
                    .into(itemView.photo)

                itemView.tv_entrance_number.text = when (photo.entranceNumber.number) {
                    -1 -> "Д"
                    else -> photo.entranceNumber.number.toString()
                }
            }
        }
    )

    fun task(onTaskClicked: (TaskItem) -> Unit): IAdapterDelegate<ReportTaskItem> = delegateDefine(
        { true },
        { p ->
            holderDefine(p, R.layout.holder_report_task, { it }) { (task, taskItem, active) ->

                itemView.btn_task.text = when(taskItem){
                    is TaskItem.Common -> "${task.name} №${task.edition}, ${taskItem.copies}экз."
                    is TaskItem.Firm -> "${task.name} №${task.edition}, ${taskItem.copies}экз., ${taskItem.firmName}, ${taskItem.office}"
                }
                if (active) {
                    itemView.btn_task.setBackgroundResource(R.drawable.abc_btn_colored_material)
                } else {
                    itemView.btn_task.setBackgroundResource(R.drawable.abc_btn_default_mtrl_shape)
                }
                when (taskItem.state) {
                    TaskItemState.CREATED -> itemView.btn_task.setTextColor(Color.parseColor("#ff000000"))
                    TaskItemState.CLOSED -> itemView.btn_task.setTextColor(Color.parseColor("#66000000"))
                }

                itemView.btn_task.setOnClickListener {
                    onTaskClicked(taskItem)
                }
            }
        }
    )

    fun entrance(
        onSelectionClicked: (entranceNumber: EntranceNumber, button: EntranceSelectionButton) -> Unit,
        onCoupleClicked: (entranceNumber: EntranceNumber) -> Unit,
        onPhotoClicked: (entranceNumber: EntranceNumber) -> Unit
    ): IAdapterDelegate<ReportEntranceItem> = delegateDefine(
        { true },
        { p ->
            holderDefine(p, R.layout.holder_report_entrance, { it }) { entrance ->
                val entranceData = entrance.taskItem.entrancesData.firstOrNull { it.number == entrance.entranceNumber }
                val apartmentsCount = entranceData?.apartmentsCount ?: "?"
                itemView.tv_entrance.text = "${entrance.entranceNumber.number}п-${apartmentsCount}"

                itemView.iv_photo.setOnClickListener {
                    onPhotoClicked(entrance.entranceNumber)
                }

                val photoImgRes = if (entrance.hasPhoto) {
                    R.drawable.ic_entrance_photo_done
                } else {
                    when (entranceData?.photoRequired) {
                        true -> R.drawable.ic_entrance_photo_req
                        else -> R.drawable.ic_entrance_photo
                    }
                }
                itemView.iv_photo.setImageResource(photoImgRes)

                itemView.tv_entrance.setOnClickListener {
                    onCoupleClicked(entrance.entranceNumber)
                }

                if (entrance.coupleEnabled) {
                    itemView.tv_entrance.setBackgroundResource(R.drawable.bg_entrance_couple_enabled)
                } else {
                    itemView.tv_entrance.setBackgroundColor(Color.parseColor("#000000ff"))
                }

                with(itemView) {
                    setSelectButtonActive(btn_euro, entrance.selection.isEuro, entranceData?.isEuroBoxes == true)
                    setSelectButtonActive(btn_watch, entrance.selection.isWatch, entranceData?.hasLookout == true)
                    setSelectButtonActive(btn_stack, entrance.selection.isStacked, entranceData?.isStacked == true)
                    setSelectButtonActive(btn_reject, entrance.selection.isRejected, entranceData?.isRefused == true)

                    btn_euro.setOnClickListener {
                        onSelectionClicked(entrance.entranceNumber, EntranceSelectionButton.Euro)
                    }
                    btn_watch.setOnClickListener {
                        onSelectionClicked(entrance.entranceNumber, EntranceSelectionButton.Watch)
                    }
                    btn_stack.setOnClickListener {
                        onSelectionClicked(entrance.entranceNumber, EntranceSelectionButton.Stack)
                    }
                    btn_reject.setOnClickListener {
                        onSelectionClicked(entrance.entranceNumber, EntranceSelectionButton.Reject)
                    }
                }
            }
        }
    )

    private fun setSelectButtonActive(button: Button, active: Boolean, hasDefaultValue: Boolean = false) {
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