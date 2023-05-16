package ru.relabs.kurjer.presentation.storageReport

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import com.bumptech.glide.Glide
import ru.relabs.kurjer.R
import ru.relabs.kurjer.databinding.HolderReportPhotoBinding
import ru.relabs.kurjer.databinding.HolderReportPhotoSingleBinding
import ru.relabs.kurjer.databinding.HolderStorageClosureBinding
import ru.relabs.kurjer.domain.models.storage.StorageReportPhoto
import ru.relabs.kurjer.presentation.base.recycler.IAdapterDelegate
import ru.relabs.kurjer.presentation.base.recycler.delegateDefine
import ru.relabs.kurjer.presentation.base.recycler.holderDefine
import ru.relabs.kurjer.uiOld.helpers.formattedTimeDate

object StorageReportAdapter {

    fun photoSingle(onPhotoClicked: () -> Unit): IAdapterDelegate<StorageReportItem> =
        delegateDefine(
            { it is StorageReportItem.Single },
            { p ->
                holderDefine(
                    p,
                    R.layout.holder_report_photo_single,
                    { it as StorageReportItem.Single }) { (required, hasPhoto) ->
                    val binding = HolderReportPhotoSingleBinding.bind(itemView)
                    binding.root.setOnClickListener {
                        onPhotoClicked()
                    }

                    binding.ivPhoto.imageTintList = ColorStateList.valueOf(
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
                    val binding = HolderReportPhotoBinding.bind(itemView)
                    binding.tvEntranceNumber.visibility = View.GONE
                    binding.ivRemove.setOnClickListener {
                        onRemoveClicked(photo)
                    }

                    Glide.with(itemView)
                        .load(uri)
                        .into(binding.ivPhoto)
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
                val binding = HolderStorageClosureBinding.bind(itemView)
                binding.tvClosureDate.text = binding.root.resources.getString(
                    R.string.closure_date,
                    idx + 1,
                    closure.closeDate.formattedTimeDate()
                )
                binding.tvClosureDescription.text = task.listName
            }
        }
    )
}