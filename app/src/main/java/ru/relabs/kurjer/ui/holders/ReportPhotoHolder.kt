package ru.relabs.kurjer.ui.holders

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_report_photo.view.*
import ru.relabs.kurjer.ui.models.ReportPhotosListModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 30.08.2018.
 */

class ReportPhotoHolder(itemView: View, private val onRemoveClicked: (holder: RecyclerView.ViewHolder) -> Unit) : BaseViewHolder<ReportPhotosListModel>(itemView) {
    override fun onBindViewHolder(item: ReportPhotosListModel) {
        if (item !is ReportPhotosListModel.TaskItemPhoto) return
        Glide.with(itemView)
                .load(item.photoURI)
                .into(itemView.photo)
        itemView.remove.setOnClickListener {
            onRemoveClicked(this)
        }
        itemView.entrance_badge.text = when (item.taskItem.entranceNumber) {
            -1 -> "Д"
            else -> item.taskItem.entranceNumber.toString()
        }
    }
}
