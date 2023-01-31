package ru.relabs.kurjer.presentation.storage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.relabs.kurjer.databinding.FragmentStorageBinding
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment

class StorageFragment : BaseFragment() {

    private lateinit var binding: FragmentStorageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStorageBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val ARG_TASK_ID = "task_id"
        fun newInstance(taskId: TaskId) = StorageFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_TASK_ID, taskId)
            }
        }
    }
}