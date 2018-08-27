package ru.relabs.kurjer.ui.delegateAdapter

import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Created by ProOrange on 11.08.2018.
 */
abstract class BaseViewHolder<T>(itemView: View): RecyclerView.ViewHolder(itemView) {
    abstract fun onBindViewHolder(item: T)
}