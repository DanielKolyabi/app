package ru.relabs.kurjer.uiOld.holders

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_report_photo_blank.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.uiOld.models.ReportPhotosListModel
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 30.08.2018.
 */
class ReportBlankPhotoHolder(itemView: View, private val onPhotoClicked: (holder: RecyclerView.ViewHolder) -> Unit) : BaseViewHolder<ReportPhotosListModel>(itemView) {
    override fun onBindViewHolder(item: ReportPhotosListModel) {
        if (item !is ReportPhotosListModel.BlankPhoto) return
        itemView.setOnClickListener {
            onPhotoClicked(this)
        }
        val photoImgRes = if (item.hasPhoto) {
            R.drawable.ic_house_photo_done
        } else {
            when (item.required) {
                true -> R.drawable.ic_house_photo_req
                else -> R.drawable.ic_house_photo
            }
        }
        itemView.blank_photo.setImageResource(photoImgRes)
    }
}