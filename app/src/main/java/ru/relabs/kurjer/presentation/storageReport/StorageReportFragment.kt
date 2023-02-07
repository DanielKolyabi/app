package ru.relabs.kurjer.presentation.storageReport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_report.*
import kotlinx.android.synthetic.main.include_hint_container.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.relabs.kurjer.R
import ru.relabs.kurjer.databinding.FragmentStorageReportBinding
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.*
import ru.relabs.kurjer.presentation.report.ListClickInterceptor
import ru.relabs.kurjer.uiOld.helpers.HintHelper
import ru.relabs.kurjer.utils.debug
import ru.relabs.kurjer.utils.extensions.showDialog
import ru.relabs.kurjer.utils.extensions.visible

class StorageReportFragment : BaseFragment() {

    private val controller = defaultController(StorageReportState(), StorageReportContext())
    private var renderJob: Job? = null
    private lateinit var binding: FragmentStorageReportBinding
    private val closesAdapter = DelegateAdapter(
        StorageReportAdapter.closure()
    )

    private val photosAdapter = DelegateAdapter(
        StorageReportAdapter.photoSingle {
            uiScope.sendMessage(controller, StorageReportMessages.msgPhotoClicked())
        },
        StorageReportAdapter.photo {
//            uiScope.sendMessage(controller, StorageReportMessages.msgRemovePhotoClicked(it))
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskIds =
            arguments?.getParcelableArrayList<TaskId>(ARG_TASK_IDS)?.toList()

        if (taskIds != null) {
            controller.start(StorageReportMessages.msgInit(taskIds))
        } else {
            controller.start(msgEmpty())
            showDialog(
                getString(R.string.unknown_runtime_error_code, "srf:100"),
                R.string.ok to {
                    uiScope.sendMessage(
                        controller,
                        StorageReportMessages.msgNavigateBack()
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
        binding = FragmentStorageReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hintHelper = HintHelper(hint_container, "", true, requireActivity())

//        val listInterceptor = ListClickInterceptor()
        binding.rvCloses.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvCloses.adapter = closesAdapter

        binding.rvPhotos.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPhotos.adapter = photosAdapter
//        binding.rvPhotos.addOnItemTouchListener(listInterceptor)


        bindControls(binding)
        renderJob = uiScope.launch {
            val renders = listOf(
                StorageReportRenders.renderLoading(binding.loading),
                StorageReportRenders.renderHint(hintHelper),
                StorageReportRenders.renderTitle(binding.tvTitle),
                StorageReportRenders.renderClosesList(closesAdapter),
                StorageReportRenders.renderPhotos(photosAdapter),
//                StorageReportRenders.renderDescription(binding.etDescription),

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

    private fun bindControls(binding: FragmentStorageReportBinding) {
        binding.ivMenu.setOnClickListener {
            uiScope.sendMessage(
                controller,
                StorageReportMessages.msgNavigateBack()
            )
        }
        binding.btnShowMap.setOnClickListener { }
        binding.btnClose.setOnClickListener { }
    }

    companion object {
        private const val ARG_TASK_IDS = "task_ids"
        fun newInstance(taskId: List<TaskId>) = StorageReportFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(ARG_TASK_IDS, ArrayList(taskId))
            }
        }
    }
}