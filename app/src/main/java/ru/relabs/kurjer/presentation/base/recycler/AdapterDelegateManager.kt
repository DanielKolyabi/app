package ru.relabs.kurjer.presentation.base.recycler

import android.view.View
import android.view.ViewGroup

class AdapterDelegateManager<T> {
    val delegates: MutableList<IAdapterDelegate<T>> = mutableListOf()

    fun getItemViewType(items: List<T>, position: Int): Int {
        return delegates.indexOfFirst { it.isForViewType(items, position) }
    }

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseHolder<T> {
        return when (viewType) {
            in 0..delegates.size -> delegates[viewType].createViewHolder(parent)
            else -> BaseHolder(View(parent.context))
        }
    }

    fun bindViewHolder(items: List<T>, position: Int, holder: BaseHolder<T>) {
        val viewType = getItemViewType(items, position)
        if (viewType >= 0 && viewType < delegates.size) {
            delegates[viewType].bindViewHolder(items, position, holder)
        }
    }

    fun onViewDetachedFromWindow(holder: BaseHolder<T>) {
        delegates.getOrNull(holder.itemViewType)?.onViewDetachedFromWindow(holder)
    }

    fun onViewAttachedToWindow(holder: BaseHolder<T>) {
        delegates.getOrNull(holder.itemViewType)?.onViewAttachedToWindow(holder)
    }

}