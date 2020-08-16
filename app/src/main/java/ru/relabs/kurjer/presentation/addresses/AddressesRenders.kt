package ru.relabs.kurjer.presentation.addresses

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import okhttp3.internal.toImmutableList
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.AddressId
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemState
import ru.relabs.kurjer.presentation.base.DefaultListDiffCallback
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.renderT
import ru.relabs.kurjer.utils.SearchUtils
import ru.relabs.kurjer.utils.extensions.getColorCompat
import ru.relabs.kurjer.utils.extensions.visible

/**
 * Created by Daniil Kurchanov on 06.04.2020.
 */
object AddressesRenders {
    fun renderLoading(view: View): AddressesRender = renderT(
        { it.loaders > 0 && it.tasks.isNotEmpty() },
        { view.visible = it }
    )

    fun renderList(adapter: DelegateAdapter<AddressesItem>): AddressesRender = renderT(
        { Triple(it.tasks, it.sorting, it.loaders > 0) to it.searchFilter },
        { (data, searchFilter) ->
            val (tasks, sorting, loading) = data
            val newItems =
                listOfNotNull(
                    AddressesItem.Sorting(sorting).takeIf { tasks.size == 1 },
                    AddressesItem.Search(searchFilter).takeIf { tasks.isNotEmpty() },
                    AddressesItem.Loading.takeIf { tasks.isEmpty() && loading }
                ) + getSortedTasks(tasks, sorting, searchFilter) + listOfNotNull(AddressesItem.Blank.takeIf { tasks.isNotEmpty() })

            val diff = DiffUtil.calculateDiff(DefaultListDiffCallback(adapter.items, newItems) { o, n ->
                if ((o is AddressesItem.Search && n is AddressesItem.Search) || (o is AddressesItem.Sorting && n is AddressesItem.Sorting && o.sorting == n.sorting)) {
                    true
                } else {
                    null
                }
            })

            adapter.items.clear()
            adapter.items.addAll(newItems)
            diff.dispatchUpdatesTo(adapter)
        }
    )

    fun renderTargetListAddress(adapter: DelegateAdapter<AddressesItem>, list: RecyclerView): AddressesRender = renderT(
        { it.selectedListAddress },
        {
            it?.let { a ->
                adapter.items
                    .indexOfFirst { item -> item is AddressesItem.AddressItem && item.taskItem.address.id == a.id }
                    .takeIf { idx -> idx > 0 }
                    ?.let { idx ->
                        list.scrollToPosition(idx)
                        list.post {
                            list?.findViewHolderForAdapterPosition(idx)?.itemView?.let {
                                flashSelectedColor(it)
                            }
                        }
                    }
            }
        }
    )

    private fun flashSelectedColor(itemView: View) {
        startFlashValueAnimator(itemView) {
            startFlashValueAnimator(itemView)
        }
    }

    private fun startFlashValueAnimator(view: View, onAnimationEnd: (() -> Unit)? = null) {
        val targetColor = view.resources.getColorCompat(R.color.colorAccent)
        val colorFrom = ColorUtils.setAlphaComponent(targetColor, 0)

        val colorAnimationTo = getValueAnimator(colorFrom, targetColor, view, 500)
        val colorAnimationFrom = getValueAnimator(targetColor, colorFrom, view, 500)
        onAnimationEnd?.let {
            colorAnimationFrom.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    it()
                }
            })
        }
        colorAnimationTo.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                colorAnimationFrom.start()
            }
        })

        colorAnimationTo.start()
    }

    private fun getValueAnimator(from: Int, to: Int, view: View?, duration: Int = 250): ValueAnimator {
        return ValueAnimator.ofObject(ArgbEvaluator(), from, to).apply {
            setDuration(duration.toLong())
            addUpdateListener { animator -> view?.setBackgroundColor(animator.animatedValue as Int) }
        }
    }

    private fun getSortedTasks(tasks: List<Task>, sorting: AddressesSortingMethod, searchFilter: String): List<AddressesItem> {
        if (tasks.isEmpty()) {
            return emptyList()
        }

        val taskItems = tasks.flatMap { task -> task.items.map { item -> task to item } }.let { items ->
            if (searchFilter.isNotEmpty()) {
                items.filter { item -> SearchUtils.isMatches(item.second.address.name, searchFilter) }
            } else {
                items
            }
        }

        if (taskItems.isEmpty()) {
            return emptyList()
        }

        val sortedItems = when (sorting) {
            AddressesSortingMethod.STANDARD -> taskItems.sortedWith(compareBy<Pair<Task, TaskItem>> { it.second.subarea }
                .thenBy { it.second.bypass }
                .thenBy { it.second.address.city }
                .thenBy { it.second.address.street }
                .thenBy { it.second.address.house }
                .thenBy { it.second.address.houseName }
                .thenBy { it.second.state }
            ).groupBy {
                it.second.address.id
            }.toList().sortedBy {
                !it.second.any { it.second.state != TaskItemState.CLOSED }
            }.toMap().flatMap {
                it.value
            }
            AddressesSortingMethod.ALPHABETIC -> taskItems.sortedWith(compareBy<Pair<Task, TaskItem>> { it.second.address.city }
                .thenBy { it.second.address.street }
                .thenBy { it.second.address.house }
                .thenBy { it.second.address.houseName }
                .thenBy { it.second.state }
            ).groupBy {
                it.second.address.id
            }.toList().sortedBy {
                !it.second.any { it.second.state != TaskItemState.CLOSED }
            }.toMap().flatMap {
                it.value
            }
        }

        var lastAddressId: AddressId? = sortedItems.first().second.address.id
        var lastAddressModel: MutableList<TaskItem> = mutableListOf(sortedItems.first().second)
        val result = mutableListOf<AddressesItem>(
            AddressesItem.GroupHeader(
                sortedItems.first().first,
                lastAddressModel.toImmutableList(),
                tasks.size == 1
            )
        )
        sortedItems.forEach {
            if (lastAddressId != it.second.address.id) {
                lastAddressId = it.second.address.id
                lastAddressModel = mutableListOf(it.second)
                result.add(AddressesItem.GroupHeader(it.first, lastAddressModel.toImmutableList(), tasks.size == 1))
            }
            lastAddressModel.add(it.second)
            result.add(AddressesItem.AddressItem(it.second, it.first))
        }
        return result
    }
}