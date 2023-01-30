package ru.relabs.kurjer.presentation.storage

import android.os.Bundle
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment

class StorageFragment : BaseFragment() {


    companion object {
        private const val ARG_TASK_ID = "task_id"
        fun newInstance(taskId: TaskId) = StorageFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_TASK_ID, taskId)
            }
        }
    }
}