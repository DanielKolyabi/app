package ru.relabs.kurjer.ui.fragments


import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_address_list.*
import kotlinx.android.synthetic.main.include_hint_container.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.R
import ru.relabs.kurjer.activity
import ru.relabs.kurjer.application
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.ui.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.ui.delegates.AddressListAddressDelegate
import ru.relabs.kurjer.ui.delegates.AddressListLoaderDelegate
import ru.relabs.kurjer.ui.delegates.AddressListSortingDelegate
import ru.relabs.kurjer.ui.delegates.AddressListTaskItemDelegate
import ru.relabs.kurjer.ui.helpers.HintHelper
import ru.relabs.kurjer.ui.models.AddressListModel
import ru.relabs.kurjer.ui.presenters.AddressListPresenter

class AddressListFragment : Fragment() {
    val presenter = AddressListPresenter(this)
    private lateinit var hintHelper: HintHelper
    private var taskIds: List<Int> = listOf()
    private var tasks: List<TaskModel> = listOf()
    val adapter = DelegateAdapter<AddressListModel>()
    var currentUserToken: String? = null

    var listScrollPosition = 0

    private var needLoadFromDatabse = false

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            val taskItemId = intent.getIntExtra("task_item_closed", 0)
            if (taskItemId != 0) {
                for (task in tasks) {
                    for (taskItem in task.items) {
                        if (taskItem.id == taskItemId) {
                            taskItem.state = TaskItemModel.CLOSED
                            presenter.updateStates(currentUserToken)
                            break
                        }
                    }
                }
            }
        }
    }
    private val intentFilter = IntentFilter("NOW")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            taskIds = it.getIntegerArrayList("task_ids")
            needLoadFromDatabse = true
        }
        activity?.registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        activity?.unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_address_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hintHelper = HintHelper(hint_container, resources.getString(R.string.address_list_hint_text), false, activity!!.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE))

        currentUserToken = (application().user as UserModel.Authorized).token

        adapter.apply {
            addDelegate(AddressListAddressDelegate(
                    {
                        this@AddressListFragment.activity()?.showYandexMap(it)
                    },
                    tasks.size == 1
            ))
            addDelegate(AddressListLoaderDelegate())
            addDelegate(AddressListTaskItemDelegate(
                    { task ->
                        presenter.onItemClicked(task)
                        listScrollPosition = (list.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                    },
                    { task -> presenter.onItemMapClicked(task) }
            ))
            addDelegate(AddressListSortingDelegate(
                    { presenter.changeSortingMethod(it) }
            ))
        }

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
        launch(UI) {
            if (needLoadFromDatabse) {
                adapter.data.add(AddressListModel.Loader)
                adapter.notifyDataSetChanged()
                loadTasksFromDatabase()
                adapter.data.clear()
            }
            if (adapter.data.size == 0) {
                adapter.data.add(AddressListModel.Loader)
                adapter.notifyDataSetChanged()

                presenter.tasks.addAll(tasks)
                presenter.applySorting()
            } else {
                adapter.data.clear()
                adapter.data.add(AddressListModel.Loader)
                adapter.notifyDataSetChanged()

                presenter.updateStates(currentUserToken)
            }
        }
    }

    public fun scrollListToSavedPosition() {
        (list.layoutManager as LinearLayoutManager).scrollToPosition(listScrollPosition)
    }

    private suspend fun loadTasksFromDatabase() {
        val db = application().database
        withContext(CommonPool) {
            tasks = taskIds.map {
                db.taskDao().getById(it)?.toTaskModel(db)
            }.filterNotNull()
            for (task in tasks) {
                for (taskItem in task.items) {
                    for (entrance in taskItem.entrances) {
                        entrance.coupleEnabled = true
                    }
                }
            }
            needLoadFromDatabse = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        data ?: return
        if (requestCode != 1 || resultCode != Activity.RESULT_OK) return

        val changedItem = data.extras.get("changed_item") as TaskItemModel
        val changedTask = data.extras.get("changed_task") as TaskModel

        presenter.onDataChanged(changedTask, changedItem)
    }

    companion object {
        fun newInstance(tasks: List<TaskModel>) =
                AddressListFragment().apply {
                    arguments = Bundle().apply {
                        putIntegerArrayList("task_ids", ArrayList(tasks.map { it.id }))
                    }
                }
    }
}

