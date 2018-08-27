package ru.relabs.kurjer.ui.delegateAdapter

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

/**
 * Created by ProOrange on 11.08.2018.
 */
class DelegateAdapter<T>: RecyclerView.Adapter<BaseViewHolder<T>>() {

    val data = mutableListOf<T>()
    val adapters = mutableListOf<IAdapterDelegate<T>>()

    override fun getItemViewType(position: Int): Int{
        return adapters.indexOfFirst {
            it.isForViewType(data, position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T> {
        return adapters[viewType].onCreateViewHolder(parent, viewType)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        adapters[getItemViewType(position)].onBindViewHolder(holder, data, position)
    }

    fun addAdapter(adapter: IAdapterDelegate<T>){
        adapters.add(adapter)
    }
}