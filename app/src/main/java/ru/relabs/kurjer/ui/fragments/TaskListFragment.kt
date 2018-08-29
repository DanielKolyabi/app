package ru.relabs.kurjer.ui.fragments


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_task_list.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.AddressModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.adapters.TaskListAdapter
import ru.relabs.kurjer.ui.helpers.HintAnimationHelper
import ru.relabs.kurjer.ui.presenters.TaskListPresenter
import java.util.*


class TaskListFragment : Fragment() {
    val presenter = TaskListPresenter(this)
    private lateinit var hintAnimationHelper: HintAnimationHelper
    val adapter = TaskListAdapter(
            { presenter.onTaskSelected(it) },
            { presenter.onTaskClicked(it) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_task_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hintAnimationHelper = HintAnimationHelper(hint_container, hint_icon)
        hint_container.setOnClickListener {
            hintAnimationHelper.changeState()
        }
        start.setOnClickListener {
            presenter.onStartClicked()
        }

        tasks_list.layoutManager = LinearLayoutManager(context)
        tasks_list.adapter = adapter
        adapter.data.clear()
        adapter.data.add(TaskModel(
                1, "Вечерняя Москва", 1250, 5, 0, Date(), Date(System.currentTimeMillis()+86400000), 1, 13, "Петров Пётр Петрович", "http://url.ru", 1,
                mutableListOf(
                        TaskItemModel(
                                AddressModel(1, "ул. Шевченко, д. 25"),
                                0, 1, listOf("Привет", "Описание", "Три"), 4
                        ),
                        TaskItemModel(
                                AddressModel(1, "ул. Шевченко, д. 25"),
                                0, 1, listOf("Привет", "Описание", "Три"), 4
                        ),
                        TaskItemModel(
                                AddressModel(1, "ул. Шевченко, д. 25"),
                                0, 1, listOf("Привет", "Описание", "Три"), 4
                        ),
                        TaskItemModel(
                                AddressModel(1, "ул. Шевченко, д. 25"),
                                0, 1, listOf("Привет", "Описание", "Три"), 4
                        ),
                        TaskItemModel(
                                AddressModel(1, "ул. Шевченко, д. 25"),
                                0, 1, listOf("Привет", "Описание", "Три"), 4
                        ),
                        TaskItemModel(
                                AddressModel(1, "ул. Шевченко, д. 25"),
                                0, 1, listOf("Привет", "Описание", "Три"), 4
                        )
                ),
                false
        ))
        adapter.notifyDataSetChanged()
    }


    companion object {
        @JvmStatic
        fun newInstance() = TaskListFragment()
    }
}
