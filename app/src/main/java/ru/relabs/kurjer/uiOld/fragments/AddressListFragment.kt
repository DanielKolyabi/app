package ru.relabs.kurjer.uiOld.fragments


import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import kotlinx.android.synthetic.main.fragment_address_list.*
import kotlinx.android.synthetic.main.include_hint_container.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.AddressModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.uiOld.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.uiOld.delegates.AddressListAddressDelegate
import ru.relabs.kurjer.uiOld.delegates.AddressListLoaderDelegate
import ru.relabs.kurjer.uiOld.delegates.AddressListSortingDelegate
import ru.relabs.kurjer.uiOld.delegates.AddressListTaskItemDelegate
import ru.relabs.kurjer.uiOld.helpers.HintHelper
import ru.relabs.kurjer.uiOld.helpers.TaskAddressSorter
import ru.relabs.kurjer.uiOld.holders.AddressListTaskItemHolder
import ru.relabs.kurjer.uiOld.models.AddressListModel
import ru.relabs.kurjer.uiOld.presenters.AddressListPresenter
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.activity
import java.util.*

class AddressListFragment : Fragment(), SearchableFragment {

    override fun onSearchItems(filter: String): List<String> {
        if (filter.contains(",")) {
            return adapter.data.asSequence()
                    .filter { it is AddressListModel.Address }
                    .filter {
                        (it as AddressListModel.Address).taskItems.first().address.name.contains(filter, true)
                    }
                    .map {
                        (it as AddressListModel.Address).taskItems.first().address.name
                    }
                    .toList()
        } else {
            return adapter.data.asSequence()
                    .filter { it is AddressListModel.Address }
                    .filter {
                        (it as AddressListModel.Address).taskItems.first().address.street.contains(filter, true)
                    }
                    .map {
                        (it as AddressListModel.Address).taskItems.first().address.street + ","
                    }
                    .distinct()
                    .toList()
        }
    }

    override fun onItemSelected(item: String, searchView: AutoCompleteTextView) {

        if (presenter.sortingMethod != TaskAddressSorter.ALPHABETIC) {
            presenter.changeSortingMethod(TaskAddressSorter.ALPHABETIC)
        }

        val itemIndex = adapter.data.indexOfFirst {
            (it as? AddressListModel.Address)?.taskItems?.first()?.address?.name?.contains(item, true)
                    ?: false
        }
        if (itemIndex < 0) {
            return
        }
        list.smoothScrollToPosition(itemIndex)
    }

    var targetAddress: AddressModel? = null
    private lateinit var hintHelper: HintHelper
    private var taskIds: List<Int> = listOf()
    private var tasks: List<TaskModel> = listOf()
    val database: AppDatabase by inject()
    val adapter = DelegateAdapter<AddressListModel>()
    val presenter = AddressListPresenter(this, database)

    var listScrollPosition = 0

    private var needLoadFromDatabse = false

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            val taskItemId = intent.getIntExtra("task_item_closed", 0)
            if (taskItemId != 0) {
                mainLoop@ for (task in tasks) {
                    for (taskItem in task.items) {
                        if (taskItem.id == taskItemId) {
                            taskItem.state = TaskItemModel.CLOSED
                            presenter.updateStates()
                            break@mainLoop
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
            taskIds = it.getIntegerArrayList("task_ids")?.toList() ?: listOf()
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

        map_button?.setOnClickListener {
            activity()?.showYandexMap(adapter.data.mapNotNull { (it as? AddressListModel.TaskItem)?.taskItem }) {
                presenter.onMapAddressClicked(it)
            }
        }

        adapter.apply {
            addDelegate(AddressListAddressDelegate(
                    {
                        this@AddressListFragment.activity()?.showYandexMap(it) {
                            presenter.onMapAddressClicked(it)
                        }
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
        GlobalScope.launch(Dispatchers.Main) {
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
                scrollToAddressIfExists()
            } else {
                adapter.data.clear()
                adapter.data.add(AddressListModel.Loader)
                adapter.notifyDataSetChanged()
                presenter.updateStates()
            }
        }
    }

    fun scrollToAddressIfExists() {
        targetAddress?.let {
            scrollToAddress(it)
        }
        targetAddress = null
    }

    private fun scrollToAddress(address: AddressModel) {
        val idx = adapter.data.indexOfFirst {
            (it as? AddressListModel.TaskItem)?.taskItem?.address?.id == address.id
        }
        if (idx < 0) {
            return
        }


        list?.scrollToPosition(idx)

        list?.post {
            val holder = list?.findViewHolderForAdapterPosition(idx) as? AddressListTaskItemHolder
            holder?.flashSelectedColor()
        }
    }

    fun scrollListToSavedPosition() {
        if (targetAddress != null) {
            scrollToAddressIfExists()
        } else {
            try {
                (list.layoutManager as LinearLayoutManager).scrollToPosition(listScrollPosition)
            } catch (e: Throwable) {
                e.printStackTrace()
                CustomLog.writeToFile(CustomLog.getStacktraceAsString(e))
            }
        }
    }

    private suspend fun loadTasksFromDatabase() {
        withContext(Dispatchers.Default) {
            tasks = taskIds.mapNotNull {
                database.taskDao().getById(it)?.toTaskModel(database)
            }.filter { it.canShowedByDate(Date()) }

            for (task in tasks) {
                for (taskItem in task.items) {
                    for (entrance in taskItem.entrances) {
                        entrance.coupleEnabled = true
                    }
                }
            }

            CustomLog.writeToFile("PhotoRequiredDebug: ${tasks.flatMap { it.items }.map { it.id.toString() + " photoRequired:" + it.needPhoto.toString() }.joinToString("; ")}")
            needLoadFromDatabse = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        data ?: return
        if (requestCode != 1 || resultCode != Activity.RESULT_OK) return

        val changedItem = data.extras?.get("changed_item") as TaskItemModel
        val changedTask = data.extras?.get("changed_task") as TaskModel

        presenter.onDataChanged(changedTask, changedItem)
    }

    companion object {
        fun newInstance(tasks: List<Int>) =
                AddressListFragment().apply {
                    arguments = Bundle().apply {
                        putIntegerArrayList("task_ids", ArrayList(tasks.map { it }))
                    }
                }
    }
}

