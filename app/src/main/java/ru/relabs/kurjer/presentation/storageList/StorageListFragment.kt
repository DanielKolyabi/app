package ru.relabs.kurjer.presentation.storageList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.relabs.kurjer.R
import ru.relabs.kurjer.databinding.FragmentStorageListBinding
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.*
import ru.relabs.kurjer.utils.debug
import ru.relabs.kurjer.utils.extensions.showDialog

class StorageListFragment : BaseFragment() {

    private val controller = defaultController(StorageListState(), StorageListContext())
    private var renderJob: Job? = null
    private lateinit var binding: FragmentStorageListBinding
    private val storageListAdapter = DelegateAdapter(
        StorageListAdapter.loadingAdapter(),
        StorageListAdapter.storageItemAdapter {
            uiScope.sendMessage(
                controller,
                StorageListMessages.msgStorageItemClicked(it)
            )
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskIds =
            arguments?.getParcelableArrayList<TaskId>(ARG_TASK_IDS)?.toList()

        if (taskIds != null) {
            controller.start(StorageListMessages.msgInit(taskIds))
        } else {
            controller.start(msgEmpty())
            showDialog(
                getString(R.string.unknown_runtime_error_code, "slf:100"),
                R.string.ok to {
                    uiScope.sendMessage(
                        controller,
                        StorageListMessages.msgNavigateBack()
                    )
                }
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStorageListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAddressesList.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvAddressesList.adapter = storageListAdapter

        bindControls(binding)

        renderJob = uiScope.launch {
            val renders = listOf(
                StorageListRenders.renderList(storageListAdapter)
            )
            launch { controller.stateFlow().collect(rendersCollector(renders)) }
            launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
    }

    private fun bindControls(binding: FragmentStorageListBinding) {
        binding.ivMenu.setOnClickListener {
            uiScope.sendMessage(
                controller,
                StorageListMessages.msgNavigateBack()
            )
        }
    }

    companion object {
        private const val ARG_TASK_IDS = "task_ids"
        fun newInstance(taskIds: List<TaskId>) = StorageListFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(ARG_TASK_IDS, ArrayList(taskIds))
            }
        }
    }
}