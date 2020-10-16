package ru.relabs.kurjer.presentation.addresses

import android.graphics.Color
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.core.widget.addTextChangedListener
import kotlinx.android.synthetic.main.holder_address_list_address.view.*
import kotlinx.android.synthetic.main.holder_address_list_other_addresses.view.*
import kotlinx.android.synthetic.main.holder_address_list_sorting.view.*
import kotlinx.android.synthetic.main.holder_address_list_task.view.*
import kotlinx.android.synthetic.main.holder_search.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemState
import ru.relabs.kurjer.presentation.base.recycler.IAdapterDelegate
import ru.relabs.kurjer.presentation.base.recycler.delegateDefine
import ru.relabs.kurjer.presentation.base.recycler.delegateLoader
import ru.relabs.kurjer.presentation.base.recycler.holderDefine
import ru.relabs.kurjer.utils.extensions.dpToPx
import ru.relabs.kurjer.utils.extensions.getColorCompat
import ru.relabs.kurjer.utils.extensions.hideKeyboard
import ru.relabs.kurjer.utils.extensions.visible

object AddressesAdapter {
    fun addressDelegate(
        onMapClicked: (addressTaskItems: List<TaskItem>) -> Unit
    ): IAdapterDelegate<AddressesItem> = delegateDefine(
        { it is AddressesItem.GroupHeader },
        { p ->
            holderDefine(p, R.layout.holder_address_list_address, { it as AddressesItem.GroupHeader }) { (items, showBypass) ->
                with(itemView) {
                    val address = if (showBypass) {
                        "${items.firstOrNull()?.subarea ?: "?"}-${items.firstOrNull()?.bypass ?: "?"} "
                    } else {
                        ""
                    } + (items.firstOrNull()?.address?.name ?: resources.getString(R.string.address_unknown))
                    tv_address.text = address
                    when (items.none { it.state == TaskItemState.CREATED }) {
                        true -> {
                            tv_address.setTextColor(Color.parseColor("#CCCCCC"))
                            iv_task_map.alpha = 0.4f
                            iv_task_map.isClickable = false
                        }
                        false -> {
                            tv_address.setTextColor(Color.parseColor("#000000"))
                            iv_task_map.alpha = 1f
                            iv_task_map.isClickable = true
                        }
                    }

                    iv_task_map.setOnClickListener { onMapClicked(items) }
                }
            }
        }
    )

    fun taskItemDelegate(
        onItemClicked: (taskItem: TaskItem, task: Task) -> Unit,
        onMapClicked: (task: Task) -> Unit
    ): IAdapterDelegate<AddressesItem> = delegateDefine(
        { it is AddressesItem.AddressItem },
        { p ->
            holderDefine(p, R.layout.holder_address_list_task, { it as AddressesItem.AddressItem }) { (taskItem, task) ->
                with(itemView) {
                    btn_task.text = "${task.name} №${task.edition}, ${taskItem.copies}экз."
                    if (taskItem.needPhoto || taskItem.entrancesData.any { it.photoRequired }) {
                        when (taskItem.state) {
                            TaskItemState.CLOSED -> {
                                iv_item_map.alpha = 0.4f
                                iv_item_map.isClickable = false
                                btn_task.setTextColor(ColorUtils.setAlphaComponent(resources.getColorCompat(R.color.colorFuchsia), 128))
                            }
                            TaskItemState.CREATED -> {
                                iv_item_map.alpha = 1f
                                iv_item_map.isClickable = true
                                btn_task.setTextColor(resources.getColorCompat(R.color.colorFuchsia))
                            }
                        }
                    } else {
                        when (taskItem.state) {
                            TaskItemState.CLOSED -> {
                                iv_item_map.alpha = 0.4f
                                iv_item_map.isClickable = false
                                btn_task.setTextColor(Color.parseColor("#66000000"))
                            }
                            TaskItemState.CREATED -> {
                                iv_item_map.alpha = 1f
                                iv_item_map.isClickable = true
                                btn_task.setTextColor(Color.parseColor("#ff000000"))
                            }
                        }
                    }

                    btn_task.setOnClickListener { onItemClicked(taskItem, task) }
                    iv_item_map.setOnClickListener { onMapClicked(task) }
                }
            }
        }
    )

    fun sortingAdapter(onSortingSelected: (AddressesSortingMethod) -> Unit): IAdapterDelegate<AddressesItem> = delegateDefine(
        { it is AddressesItem.Sorting },
        { p ->
            holderDefine(p, R.layout.holder_address_list_sorting, { it as AddressesItem.Sorting }) { (sorting) ->
                when (sorting) {
                    AddressesSortingMethod.STANDARD -> {
                        itemView.btn_standart.setBackgroundColor(itemView.resources.getColorCompat(R.color.colorAccent))
                        itemView.btn_alphabetic.setBackgroundColor(itemView.resources.getColorCompat(R.color.button_material_light))
                    }
                    AddressesSortingMethod.ALPHABETIC -> {
                        itemView.btn_standart.setBackgroundColor(itemView.resources.getColorCompat(R.color.button_material_light))
                        itemView.btn_alphabetic.setBackgroundColor(itemView.resources.getColorCompat(R.color.colorAccent))
                    }
                }

                itemView.btn_standart.setOnClickListener {
                    if (sorting != AddressesSortingMethod.STANDARD) {
                        onSortingSelected(AddressesSortingMethod.STANDARD)
                    }
                }
                itemView.btn_alphabetic.setOnClickListener {
                    if (sorting != AddressesSortingMethod.ALPHABETIC) {
                        onSortingSelected(AddressesSortingMethod.ALPHABETIC)
                    }
                }
            }
        }
    )

    fun loaderAdapter(): IAdapterDelegate<AddressesItem> =
        delegateLoader { it is AddressesItem.Loading }

    fun blankAdapter(): IAdapterDelegate<AddressesItem> = delegateDefine(
        { it is AddressesItem.Blank },
        { p ->
            holderDefine(p, R.layout.holder_empty, { it as AddressesItem.Blank }) {
                itemView.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, itemView.context.dpToPx(56).toInt())
            }
        }
    )

    fun otherAddressesAdapter(onClick: () -> Unit): IAdapterDelegate<AddressesItem> = delegateDefine(
        { it is AddressesItem.OtherAddresses },
        { p ->
            holderDefine(p, R.layout.holder_address_list_other_addresses, { it as AddressesItem.OtherAddresses }) { (count) ->
                itemView.tv_more.text = itemView.resources.getString(R.string.addresses_more, count)
                itemView.setOnClickListener {
                    onClick()
                }
            }
        }
    )

    fun searchAdapter(onSearch: (String) -> Unit): IAdapterDelegate<AddressesItem> = delegateDefine(
        { it is AddressesItem.Search },
        { p ->
            holderDefine(p, R.layout.holder_search, { it as AddressesItem.Search }) { (filter) ->
                itemView.et_search.setText(filter)
                itemView.iv_clear.visible = filter.isNotBlank()

                itemView.et_search.addTextChangedListener {
                    val text = (it?.toString() ?: "")
                    itemView.iv_clear.visible = text.isNotBlank()
                    onSearch(text)
                }
                if (itemView.et_search.text.isNotEmpty()) {
                    itemView.et_search.requestFocus()
                }
                itemView.et_search.setOnEditorActionListener { _, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        actionId == EditorInfo.IME_ACTION_NEXT ||
                        event != null &&
                        event.action == KeyEvent.ACTION_DOWN &&
                        event.keyCode == KeyEvent.KEYCODE_ENTER
                    ) {
                        itemView.hideKeyboard(itemView.context)
                        true
                    }
                    false
                }
                itemView.iv_clear.setOnClickListener {
                    itemView.et_search.setText("")
                    itemView.hideKeyboard(itemView.context)
                }
            }
        }
    )
}