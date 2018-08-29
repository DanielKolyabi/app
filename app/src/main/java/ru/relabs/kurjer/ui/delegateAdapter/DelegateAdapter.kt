package ru.relabs.kurjer.ui.delegateAdapter

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

/**
 * Created by ProOrange on 11.08.2018.
 */
class DelegateAdapter<T>: RecyclerView.Adapter<BaseViewHolder<T>>() {

    val data = mutableListOf<T>()
    val delegates = mutableListOf<IAdapterDelegate<T>>()

    override fun getItemViewType(position: Int): Int{
        return delegates.indexOfFirst {
            it.isForViewType(data, position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T> {
        return delegates[viewType].onCreateViewHolder(parent, viewType)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        delegates[getItemViewType(position)].onBindViewHolder(holder, data, position)
    }

    fun addDelegate(delegate: IAdapterDelegate<T>){
        delegates.add(delegate)
    }
}