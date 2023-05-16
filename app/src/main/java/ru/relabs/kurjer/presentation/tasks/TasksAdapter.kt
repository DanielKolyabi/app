package ru.relabs.kurjer.presentation.tasks

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.text.Html
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import ru.relabs.kurjer.R
import ru.relabs.kurjer.databinding.HolderSearchBinding
import ru.relabs.kurjer.databinding.HolderTaskBinding
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskState
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
                val binding = HolderTaskBinding.bind(itemView)

                    binding.tvTitle.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(task.displayName, Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        Html.fromHtml(task.displayName)
                    }
                    when (isSelected) {
                        true -> binding.ivSelected.setImageResource(R.drawable.ic_chain_enabled)
                        false -> binding.ivSelected.setImageResource(R.drawable.ic_chain_disabled)
                    }
                    binding.ivActive.visible = task.state.state != TaskState.CREATED
                    when (task.state.byOtherUser) {
                        true ->  binding.ivActive.setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN)
                        false ->  binding.ivActive.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY)
                    }
                    binding.ivSelected.setOnClickListener {
                        onSelectedClicked(task)
                    }
                    binding.root.setOnClickListener {
                        onTaskClicked(task)
                    }
                    when (isTasksWithSameAddressPresented) {
                        true -> binding.root.setBackgroundColor(Color.GRAY)
                        else -> binding.root.setBackgroundColor(Color.TRANSPARENT)
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
            holderDefine(p, R.layout.holder_search, { it as TasksItem.Search }) { (filter) ->
                val binding = HolderSearchBinding.bind(itemView)
                binding.etSearch.setText(filter)
                binding.ivClear.visible = filter.isNotBlank()

                binding.etSearch.addTextChangedListener {
                    val text = (it?.toString() ?: "")
                    binding.ivClear.visible = text.isNotBlank()
                    onSearch(text)
                }
                if (binding.etSearch.text.isNotEmpty()) {
                    binding.etSearch.requestFocus()
                }
                binding.etSearch.setOnEditorActionListener { _, actionId, event ->
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
                binding.ivClear.setOnClickListener {
                    binding.etSearch.setText("")
                    itemView.hideKeyboard(itemView.context)
                }
            }
        }
    )
}