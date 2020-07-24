package ru.relabs.kurjer.uiOld.holders

import android.view.View
import kotlinx.android.synthetic.main.item_addr_list_sorting.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.uiOld.helpers.TaskAddressSorter
import ru.relabs.kurjer.uiOld.models.AddressListModel

/**
 * Created by ProOrange on 29.08.2018.
 */
class AddressListSortingHolder(itemView: View, val onSortingChanged: (sortingMethod: Int) -> Unit) : BaseViewHolder<AddressListModel>(itemView) {
    override fun onBindViewHolder(item: AddressListModel) {
        if (item !is AddressListModel.SortingItem) return
        itemView.sort_alphabetic.setOnClickListener {
            onSortingChanged(TaskAddressSorter.ALPHABETIC)
            setEnabledSorting(TaskAddressSorter.ALPHABETIC)
        }
        itemView.sort_standart.setOnClickListener {
            onSortingChanged(TaskAddressSorter.STANDART)
            setEnabledSorting(TaskAddressSorter.STANDART)
        }
        setEnabledSorting(item.sortType) //default
    }

    fun setEnabledSorting(sortingMethod: Int) {
        if (sortingMethod == TaskAddressSorter.STANDART) {
            itemView.sort_standart.setBackgroundColor(itemView.resources.getColor(R.color.colorAccent))
            itemView.sort_alphabetic.setBackgroundColor(itemView.resources.getColor(R.color.button_material_light))
        }
        if (sortingMethod == TaskAddressSorter.ALPHABETIC) {
            itemView.sort_alphabetic.setBackgroundColor(itemView.resources.getColor(R.color.colorAccent))
            itemView.sort_standart.setBackgroundColor(itemView.resources.getColor(R.color.button_material_light))
        }
    }
}