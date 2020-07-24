package ru.relabs.kurjer.presentation.base.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

open class BaseHolder<HolderElement>(view: View) : RecyclerView.ViewHolder(view) {

    constructor(parent: ViewGroup, @LayoutRes layout: Int) :
            this(LayoutInflater.from(parent.context).inflate(layout, parent, false))

    open fun bindModel(item: HolderElement) {}
}