package ru.relabs.kurjer.presentation.storageReport

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.holder_report_photo.view.*
import kotlinx.android.synthetic.main.holder_report_photo_single.view.*
import kotlinx.android.synthetic.main.holder_storage_closure.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.storage.StorageReportPhoto
import ru.relabs.kurjer.presentation.base.recycler.IAdapterDelegate
import ru.relabs.kurjer.presentation.base.recycler.delegateDefine
import ru.relabs.kurjer.presentation.base.recycler.holderDefine
import ru.relabs.kurjer.uiOld.helpers.formattedTimeDate
import kotlinx.android.synthetic.main.holder_report_photo.view.iv_photo as photo
import kotlinx.android.synthetic.main.holder_report_photo_single.view.iv_photo as singlePhoto

object StorageReportAdapter {

    fun photoSingle(onPhotoClicked: () -> Unit): IAdapterDelegate<StorageReportItem> =
        delegateDefine(
            { it is StorageReportItem.Single },
            { p ->
                holderDefine(
                    p,
                    R.layout.holder_report_photo_single,
                    { it as StorageReportItem.Single }) { (required, hasPhoto) ->
                    itemView.setOnClickListener {
                        onPhotoClicked()
                    }

                    itemView.singlePhoto.imageTintList = ColorStateList.valueOf(
                        when {
                            hasPhoto -> Color.parseColor("#FF435CDC")
                            required -> Color.parseColor("#FFED0D81")
                            else -> Color.BLACK
                        }
                    )
                }
            }
        )

    fun photo(onRemoveClicked: (StorageReportPhoto) -> Unit): IAdapterDelegate<StorageReportItem> =
        delegateDefine(
            { it is StorageReportItem.Photo },
            { p ->
                holderDefine(
                    p,
                    R.layout.holder_report_photo,
                    { it as StorageReportItem.Photo }) { (photo, uri) ->
                    itemView.tv_entrance_number.visibility = View.GONE
                    itemView.iv_remove.setOnClickListener {
                        onRemoveClicked(photo)
                    }

                    Glide.with(itemView)
                        .load(uri)
                        .into(itemView.photo)
                }
            }
        )

    fun closure(): IAdapterDelegate<StorageReportItem> = delegateDefine(
        { it is StorageReportItem.Closure },
        { p ->
            holderDefine(
                p,
                R.layout.holder_storage_closure,
                { it as StorageReportItem.Closure }) { (idx, task, closure) ->
                with(itemView) {
                    tv_closure_date.text = resources.getString(
                        R.string.closure_date,
                        idx + 1,
                        closure.closeDate.formattedTimeDate()
                    )
                    tv_closure_description.text = task.listName
                }
            }
        }
    )
}