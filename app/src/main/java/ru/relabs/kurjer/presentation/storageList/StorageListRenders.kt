package ru.relabs.kurjer.presentation.storageList

import androidx.recyclerview.widget.DiffUtil
import ru.relabs.kurjer.presentation.base.DefaultListDiffCallback
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.renderT

object StorageListRenders {
    fun renderList(adapter: DelegateAdapter<StorageListItem>): StorageListRender = renderT(
        { it.tasks to it.loaders },
        { (tasks, loaders) ->
            val newItems = if (loaders > 0) {
                listOf(StorageListItem.Loader)
            } else {
                tasks.groupBy { it.storage.id.id }
                    .map {
                        StorageListItem.StorageAddress(
                            it.value.first().storage,
                            it.value
                        )
                    }
            }

            val diff = DiffUtil.calculateDiff(DefaultListDiffCallback(adapter.items, newItems))
            adapter.items.clear()
            adapter.items.addAll(newItems)
            diff.dispatchUpdatesTo(adapter)
        }
    )
}