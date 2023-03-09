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
    fun storageItemAdapter(onClick: (taskIds: List<TaskId>) -> Unit): IAdapterDelegate<StorageListItem> =
        delegateDefine(
            { it is StorageListItem.StorageAddress },
            { p ->
                holderDefine(
                    p,
                    R.layout.holder_storage_list_item,
                    { it as StorageListItem.StorageAddress }) { (storage, tasks) ->
                    with(itemView) {
                        tv_storage_address.text = storage.address
                        if (tasks.any { it.isStorageActuallyRequired }) {
                            tv_storage_address.setTextColor(resources.getColorCompat(R.color.colorFuchsia))
                        } else {
                            tv_storage_address.setTextColor(resources.getColorCompat(R.color.black))
                        }
                        tv_storage_description.text = tasks.joinToString("\n") { it.task.storageListName }
                        setOnClickListener { onClick(tasks.map { it.task.id }) }
                    }
                }
            }
        )

    fun loadingAdapter(): IAdapterDelegate<StorageListItem> =
        delegateLoader { it is StorageListItem.Loader }
}