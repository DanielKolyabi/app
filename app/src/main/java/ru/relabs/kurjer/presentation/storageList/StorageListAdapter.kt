package ru.relabs.kurjer.presentation.storageList

import kotlinx.android.synthetic.main.holder_storage_list_item.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.presentation.base.recycler.IAdapterDelegate
import ru.relabs.kurjer.presentation.base.recycler.delegateDefine
import ru.relabs.kurjer.presentation.base.recycler.delegateLoader
import ru.relabs.kurjer.presentation.base.recycler.holderDefine
import ru.relabs.kurjer.utils.extensions.getColorCompat

object StorageListAdapter {
    fun storageItemAdapter(onClick: (taskId: TaskId) -> Unit): IAdapterDelegate<StorageListItem> =
        delegateDefine(
            { it is StorageListItem.StorageItem },
            { p ->
                holderDefine(
                    p,
                    R.layout.holder_storage_list_item,
                    { it as StorageListItem.StorageItem }) { item ->
                    with(itemView) {
                        tv_storage_address.text = item.task.storage.address
                        if (item.task.storageCloseFirstRequired) {
                            tv_storage_address.setTextColor(resources.getColorCompat(R.color.colorFuchsia))
                        } else {
                            tv_storage_address.setTextColor(resources.getColorCompat(R.color.black))
                        }
                        tv_storage_description.text = item.task.listName
                        setOnClickListener { onClick(item.task.id) }
                    }
                }
            }
        )

    fun loadingAdapter(): IAdapterDelegate<StorageListItem> =
        delegateLoader { it is StorageListItem.Loader }
}