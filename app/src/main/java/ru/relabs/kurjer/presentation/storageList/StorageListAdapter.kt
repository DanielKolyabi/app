package ru.relabs.kurjer.presentation.storageList

import ru.relabs.kurjer.R
import ru.relabs.kurjer.databinding.HolderStorageListItemBinding
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
                        val binding = HolderStorageListItemBinding.bind(itemView)
                    binding.tvStorageAddress.text = storage.address
                        if (tasks.any { it.isStorageActuallyRequired }) {
                            binding.tvStorageAddress.setTextColor(binding.root.resources.getColorCompat(R.color.colorFuchsia))
                        } else {
                            binding.tvStorageAddress.setTextColor(binding.root.resources.getColorCompat(R.color.black))
                        }
                        binding.tvStorageDescription.text = tasks.joinToString("\n") { it.task.storageListName }
                        binding.root.setOnClickListener { onClick(tasks.map { it.task.id }) }

                }
            }
        )

    fun loadingAdapter(): IAdapterDelegate<StorageListItem> =
        delegateLoader { it is StorageListItem.Loader }
}