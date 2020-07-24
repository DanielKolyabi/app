package ru.relabs.kurjer.presentation.base.recycler

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

class DelegateAdapter<T>(vararg delegate: IAdapterDelegate<T>) : RecyclerView.Adapter<BaseHolder<T>>() {

    val items: MutableList<T> = mutableListOf()
    private val manager = AdapterDelegateManager<T>()

    init {
        manager.delegates.addAll(delegate)
    }

    override fun onViewAttachedToWindow(holder: BaseHolder<T>) {
        super.onViewAttachedToWindow(holder)
        manager.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: BaseHolder<T>) {
        super.onViewDetachedFromWindow(holder)
        manager.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: BaseHolder<T>, position: Int) {
        manager.bindViewHolder(items, position, holder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseHolder<T> {
        return manager.createViewHolder(parent, viewType)
    }

    override fun getItemViewType(position: Int): Int = manager.getItemViewType(items, position)
}


fun <T> delegateDefine(
    check: (T) -> Boolean,
    holder: (ViewGroup) -> BaseHolder<T>
): IAdapterDelegate<T> = object : IAdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return check(items[position])
    }

    override fun createViewHolder(parent: ViewGroup): BaseHolder<T> {
        return holder(parent)
    }
}

fun <T, E> holderDefine(
    parent: ViewGroup, @LayoutRes layout: Int,
    transform: (T) -> E,
    bind: BaseHolder<T>.(E) -> Unit
): BaseHolder<T> = object : BaseHolder<T>(parent, layout) {
    override fun bindModel(item: T) = bind(transform(item))
}

fun <T> holderDefine(parent: ViewGroup, @LayoutRes layout: Int, bind: BaseHolder<T>.(T) -> Unit) =
    holderDefine(parent, layout, { it }, bind)

//fun <T> delegateLoader(check: (T) -> Boolean): IAdapterDelegate<T> = delegateDefine(check) {
//    holderDefine(it, R.layout.item_loader) {}
//}