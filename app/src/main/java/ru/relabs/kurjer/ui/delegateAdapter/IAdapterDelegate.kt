package ru.relabs.kurjer.ui.delegateAdapter

import android.view.ViewGroup

/**
 * Created by ProOrange on 11.08.2018.
 */

interface IAdapterDelegate<T> {

    fun isForViewType(data: List<T>, position: Int): Boolean
    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T>
    fun onBindViewHolder(holder: BaseViewHolder<T>, data: List<T>, position: Int)
}