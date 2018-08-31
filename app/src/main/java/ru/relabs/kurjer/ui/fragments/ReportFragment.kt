package ru.relabs.kurjer.ui.fragments


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_report.*
import kotlinx.android.synthetic.main.item_report_photo.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.*
import ru.relabs.kurjer.ui.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.ui.delegates.ReportBlankPhotoDelegate
import ru.relabs.kurjer.ui.delegates.ReportEntrancesDelegate
import ru.relabs.kurjer.ui.delegates.ReportPhotoDelegate
import ru.relabs.kurjer.ui.delegates.ReportTasksDelegate
import ru.relabs.kurjer.ui.helpers.HintAnimationHelper
import ru.relabs.kurjer.ui.helpers.setVisible
import ru.relabs.kurjer.ui.presenters.ReportPresenter

class ReportFragment : Fragment() {
    lateinit var tasks: List<TaskModel>
    lateinit var taskItems: List<TaskItemModel>
    private lateinit var hintAnimationHelper: HintAnimationHelper

    val tasksListAdapter = DelegateAdapter<ReportTasksListModel>()
    val entrancesListAdapter = DelegateAdapter<ReportEntrancesListModel>()
    val photosListAdapter = DelegateAdapter<ReportPhotosListModel>()

    private val presenter = ReportPresenter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tasks = it.getParcelableArrayList("tasks")
            taskItems = it.getParcelableArrayList("task_items")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hintAnimationHelper = HintAnimationHelper(hint_container, hint_icon)
        hint_container.setOnClickListener {
            hintAnimationHelper.changeState()
        }

        tasksListAdapter.addDelegate(ReportTasksDelegate({
            presenter.changeCurrentTask(it)
        }))
        entrancesListAdapter.addDelegate(ReportEntrancesDelegate())
        photosListAdapter.addDelegate(ReportPhotoDelegate())
        photosListAdapter.addDelegate(ReportBlankPhotoDelegate())

        tasks_list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        entrances_list.layoutManager = LinearLayoutManager(context)
        photos_list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        tasks_list.adapter = tasksListAdapter
        entrances_list.adapter = entrancesListAdapter
        photos_list.adapter = photosListAdapter

        presenter.fillTasksAdapterData()
        presenter.changeCurrentTask(0)
    }

    fun showHintText(notes: List<String>) {
        hint_text.text = Html.fromHtml("<b><h3>Пр. 1</h3></b>\n" + notes.reduceIndexed { i, acc, s ->
            acc + (if (i == 1) "\n" else "") + "<b><h3>Пр. ${i + 1}</h3></b>\n$s\n"
        })
    }

    fun setTaskListVisible(visible: Boolean) {
        tasks_list.setVisible(true)
    }

    fun setTaskListActiveTask(taskNumber: Int, isActive: Boolean) {
        (tasksListAdapter.data[taskNumber] as? ReportTasksListModel.TaskButton)?.active = isActive
        tasksListAdapter.notifyItemChanged(taskNumber)
    }

    companion object {
        @JvmStatic
        fun newInstance(task: List<TaskModel>, taskItem: List<TaskItemModel>) =
                ReportFragment().apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList("tasks", ArrayList(task))
                        putParcelableArrayList("task_items", ArrayList(taskItem))
                    }
                }
    }
}
