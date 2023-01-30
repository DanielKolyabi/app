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
import ru.relabs.kurjer.presentation.biba.BibaContext
import ru.relabs.kurjer.presentation.biba.BibaState
import ru.relabs.kurjer.utils.debug
import ru.relabs.kurjer.utils.extensions.showDialog

class StorageListFragment : BaseFragment() {

    private val controller = defaultController(BibaState(), BibaContext())
    private var renderJob: Job? = null
    private lateinit var binding: FragmentStorageListBinding
    private val storageListAdapter = DelegateAdapter(

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
                getString(R.string.unknown_runtime_error_code, "af:100"),
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
            val renders = listOf<>(

            )
            launch { controller.stateFlow().collect(rendersCollector(renders)) }
            launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
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