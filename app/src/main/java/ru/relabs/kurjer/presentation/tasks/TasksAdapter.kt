package ru.relabs.kurjer.presentation.tasks

import android.graphics.Color
import android.graphics.PorterDuff
import androidx.core.widget.addTextChangedListener
import kotlinx.android.synthetic.main.holder_search.view.*
import kotlinx.android.synthetic.main.holder_task.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskState
import ru.relabs.kurjer.presentation.addresses.AddressesItem
import ru.relabs.kurjer.presentation.base.recycler.IAdapterDelegate
import ru.relabs.kurjer.presentation.base.recycler.delegateDefine
import ru.relabs.kurjer.presentation.base.recycler.delegateLoader
import ru.relabs.kurjer.presentation.base.recycler.holderDefine
import ru.relabs.kurjer.utils.extensions.hideKeyboard
import ru.relabs.kurjer.utils.extensions.visible

object TasksAdapter {
    fun taskAdapter(
        onSelectedClicked: (task: Task) -> Unit,
        onTaskClicked: (task: Task) -> Unit
    ): IAdapterDelegate<TasksItem> = delegateDefine(
        { it is TasksItem.TaskItem },
        { p ->
            holderDefine(p, R.layout.holder_task, { it as TasksItem.TaskItem }) { (task, isTasksWithSameAddressPresented, isSelected) ->
                with(itemView) {
                    tv_title.text = task.listName
                    when (isSelected) {
                        true -> iv_selected.setImageResource(R.drawable.ic_chain_enabled)
                        false -> iv_selected.setImageResource(R.drawable.ic_chain_disabled)
                    }
                    iv_active.visible = task.state.state != TaskState.CREATED
                    when (task.state.byOtherUser) {
                        true -> iv_active.setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN)
                        false -> iv_active.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY)
                    }
                    iv_selected.setOnClickListener {
                        onSelectedClicked(task)
                    }
                    setOnClickListener {
                        onTaskClicked(task)
                    }
                    when (isTasksWithSameAddressPresented) {
                        true -> setBackgroundColor(Color.GRAY)
                        else -> setBackgroundColor(Color.TRANSPARENT)
                    }
                }
            }
        }
    )

    fun loaderAdapter(): IAdapterDelegate<TasksItem> =
        delegateLoader { it is TasksItem.Loader }

    fun blankAdapter(): IAdapterDelegate<TasksItem> = delegateDefine(
        { it is TasksItem.Blank },
        { p ->
            holderDefine(p, R.layout.holder_empty, { it as TasksItem.Blank }) {}
        }
    )

    fun searchAdapter(onSearch: (String) -> Unit): IAdapterDelegate<TasksItem> = delegateDefine(
        { it is TasksItem.Search },
        { p ->
            holderDefine(p, R.layout.holder_search, { it as TasksItem.Search }) {
                itemView.et_search.addTextChangedListener {
                    val text = (it?.toString() ?: "")
                    itemView.iv_clear.visible = text.isNotBlank()
                    onSearch(text)
                }
                itemView.iv_clear.setOnClickListener {
                    itemView.et_search.setText("")
                    itemView.hideKeyboard(itemView.context)
                }
            }
        }
    )
}