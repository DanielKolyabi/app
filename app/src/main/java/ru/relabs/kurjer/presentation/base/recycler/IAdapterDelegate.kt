package ru.relabs.kurjer.presentation.base.recycler

import android.view.ViewGroup

interface IAdapterDelegate<T> {
    fun isForViewType(items: List<T>, position: Int): Boolean
    fun createViewHolder(parent: ViewGroup): BaseHolder<T>

    fun bindViewHolder(items: List<T>, position: Int, holder: BaseHolder<T>) {
        holder.bindModel(items[position])
    }
    fun onViewAttachedToWindow(holder: BaseHolder<T>) {}
    fun onViewDetachedFromWindow(holder: BaseHolder<T>) {}
}

