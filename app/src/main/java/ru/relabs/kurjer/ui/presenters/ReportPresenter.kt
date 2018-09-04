package ru.relabs.kurjer.ui.presenters

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.fragment_report.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.ErrorButtonsListener
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.MyApplication
import ru.relabs.kurjer.files.PathHelper.getTaskItemPhotoFile
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.models.TaskItemResultEntranceModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.entities.TaskItemPhotoEntity
import ru.relabs.kurjer.persistence.entities.TaskItemResultEntity
import ru.relabs.kurjer.persistence.entities.TaskItemResultEntranceEntity
import ru.relabs.kurjer.ui.fragments.ReportFragment
import ru.relabs.kurjer.ui.models.ReportEntrancesListModel
import ru.relabs.kurjer.ui.models.ReportPhotosListModel
import ru.relabs.kurjer.ui.models.ReportTasksListModel
import java.io.File
import java.io.FileOutputStream
import java.util.*


const val REQUEST_PHOTO = 1

class ReportPresenter(private val fragment: ReportFragment) {

    var currentTask = 0

    fun changeCurrentTask(taskNumber: Int) {
        fragment.setTaskListActiveTask(currentTask, false)
        fragment.setTaskListActiveTask(taskNumber, true)
        currentTask = taskNumber
        fragment.showHintText(fragment.taskItems[currentTask].notes)
        launch {
            val db = (fragment.activity!!.application as MyApplication).database
            fillEntrancesAdapterData(db)
            fillPhotosAdapterData(db)
            fillDescriptionData(db)
            withContext(UI){
                fragment.loading.visibility = View.GONE
            }
        }

        (fragment.context as? MainActivity)?.changeTitle(fragment.taskItems[currentTask].address.name)
    }

    private fun fillDescriptionData(db: AppDatabase) {
        db.taskItemResultsDao().getByTaskItemId(fragment.taskItems[currentTask].id)?.let {
            launch(UI) {
                fragment.user_explanation_input.setText(it.description)
            }
        }
    }

    fun fillTasksAdapterData() {
        fragment.tasksListAdapter.data.addAll(fragment.tasks.mapIndexed { i, it ->
            ReportTasksListModel.TaskButton(it, i, i == 0)
        })
        fragment.tasksListAdapter.notifyDataSetChanged()
    }


    fun fillEntrancesAdapterData(db: AppDatabase) {
        val savedEntrancesAtSameAddress = findEntrancesByAddress(db)
        //create entrance models with loading state from saved address
        val entrances = fragment.taskItems[currentTask].entrances.map {
            val savedEntrance = savedEntrancesAtSameAddress.firstOrNull { saved ->
                it == saved.entrance
            }
            val savedState = if(savedEntrance == null) 0 else savedEntrance.state

            ReportEntrancesListModel.Entrance(fragment.taskItems[currentTask], it, savedState)
        }
        //find saved state directly for this entrances
        db.taskItemResultsDao().getByTaskItemId(fragment.taskItems[currentTask].id)?.let {
            val savedEntrances = db.entrancesDao().getByTaskItemResultId(it.id)
            entrances.map { ent ->
                ent.selected = savedEntrances.first { it.entrance == ent.entranceNumber }.state
            }
        }

        launch(UI) {
            fragment.entrancesListAdapter.data.clear()
            if (fragment.tasks.size > 1) {
                fragment.setTaskListVisible(true)
            }
            fragment.entrancesListAdapter.data.addAll(entrances)
            fragment.entrancesListAdapter.notifyDataSetChanged()
        }
    }

    private fun findEntrancesByAddress(db: AppDatabase): List<TaskItemResultEntranceModel> {
        val result = mutableListOf<TaskItemResultEntranceModel>()
        db.taskItemDao().getByAddressId(fragment.taskItems[currentTask].address.id).filter {
            it.taskId in fragment.tasks.map { it.id }
        }.forEach {
            db.taskItemResultsDao().getByTaskItemId(it.id)?.let {
                db.entrancesDao().getByTaskItemResultId(it.id).filter{
                    it.state != 0
                }.forEach {
                    result.add(it.toTaskItemResultEntranceModel())
                }
            }
        }
        return result
    }

    fun fillPhotosAdapterData(db: AppDatabase) {
        fragment.photosListAdapter.data.clear()
        val taskPhotos = db.photosDao().getByTaskItemId(fragment.taskItems[currentTask].id).map {
            it.toTaskItemPhotoModel(db)
        }

        launch(UI) {
            taskPhotos.forEach {
                fragment.photosListAdapter.data.add(
                        ReportPhotosListModel.TaskItemPhoto(it, it.getPhotoURI())
                )
            }
            fragment.photosListAdapter.data.add(ReportPhotosListModel.BlankPhoto)
            fragment.photosListAdapter.notifyDataSetChanged()
        }
    }

    fun onEntranceSelected(type: Int, holder: RecyclerView.ViewHolder) {
        val data = (fragment.entrancesListAdapter.data[holder.adapterPosition] as ReportEntrancesListModel.Entrance)
        data.selected = data.selected xor type
        launch(CommonPool) {
            createOrUpdateTaskResult()
        }
        fragment.entrancesListAdapter.notifyItemChanged(holder.adapterPosition)
    }

    private fun createOrUpdateTaskResult() {
        val db = (fragment.activity!!.application as MyApplication).database
        val taskItemEntity = db.taskItemDao().getById(fragment.taskItems[currentTask].id)
        val taskItemResult = db.taskItemResultsDao().getByTaskItemId(taskItemEntity.id)
        if (taskItemResult == null) {
            createTaskItemResult(db)
        } else {
            updateTaskItemResult(db)
        }
    }

    private fun createTaskItemResult(db: AppDatabase) {
        val newId = db.taskItemResultsDao().insert(
                TaskItemResultEntity(
                        0,
                        fragment.taskItems[currentTask].id,
                        GPSCoordinatesModel(0.0, 0.0, Date()),
                        null,
                        fragment.user_explanation_input.text.toString()
                )
        )
        db.entrancesDao().insertAll(
                fragment.entrancesListAdapter.data.filter { it is ReportEntrancesListModel.Entrance }.map {
                    TaskItemResultEntranceEntity(
                            0,
                            newId.toInt(),
                            (it as ReportEntrancesListModel.Entrance).entranceNumber,
                            it.selected
                    )
                }
        )
    }

    private fun updateTaskItemResult(db: AppDatabase) {
        val result = db.taskItemResultsDao().getByTaskItemId(fragment.taskItems[currentTask].id)
        val entrances = db.entrancesDao().getByTaskItemResultId(result!!.id)
        result.description = fragment.user_explanation_input.text.toString()
        entrances.map {
            it.state = (fragment.entrancesListAdapter.data.filter { it is ReportEntrancesListModel.Entrance }.first { entData ->
                (entData as ReportEntrancesListModel.Entrance).entranceNumber == it.entrance
            } as ReportEntrancesListModel.Entrance).selected
        }

        db.taskItemResultsDao().update(result)
        entrances.forEach { db.entrancesDao().update(it) }
    }


    fun onBlankPhotoClicked(holder: RecyclerView.ViewHolder) {
        requestPhoto()
    }

    private fun requestPhoto() {

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (intent.resolveActivity(fragment.context?.packageManager) != null) {
            fragment.startActivityForResult(intent, REQUEST_PHOTO)
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_PHOTO) {
            if (resultCode != RESULT_OK) {
                (fragment.context as MainActivity).showError("Не удалось сделать фото.")
                return false
            }

            if (data == null) {
                (fragment.context as MainActivity).showError("Не удалось сделать фото.")
                return false
            }

            val photo = saveNewPhoto(data.extras!!.get("data") as Bitmap)

            if (photo == null) {
                (fragment.context as MainActivity).showError("Не удалось сделать фото.")
                return false
            }

            return true
        }
        return false
    }

    private fun saveNewPhoto(bmp: Bitmap): File? {
        val uuid = UUID.randomUUID()
        val photoFile = getTaskItemPhotoFile(fragment.taskItems[currentTask], uuid)

        MediaStore.Images.Media.insertImage(fragment.context!!.contentResolver, bmp, null, null)

        try {
            val fos = FileOutputStream(photoFile)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        bmp.recycle()
        launch(UI) {
            val db = (fragment.activity!!.application as MyApplication).database
            val photoEntity = TaskItemPhotoEntity(0, uuid.toString(), GPSCoordinatesModel(0.toDouble(), 0.toDouble(), Date()), fragment.taskItems[currentTask].id)
            val photoModel = withContext(CommonPool) {
                val id = db.photosDao().insert(photoEntity)
                db.photosDao().getById(id.toInt()).toTaskItemPhotoModel(db)
            }

            fragment.photosListAdapter.data.add(fragment.photosListAdapter.data.size - 1,
                    ReportPhotosListModel.TaskItemPhoto(
                            photoModel,
                            Uri.fromFile(photoFile)
                    )
            )
            fragment.photosListAdapter.notifyItemRangeChanged(fragment.photosListAdapter.data.size - 1, 2)

            requestPhoto()
        }
        return photoFile
    }

    fun onRemovePhotoClicked(holder: RecyclerView.ViewHolder) {
        val status = File((fragment.photosListAdapter.data[holder.adapterPosition] as ReportPhotosListModel.TaskItemPhoto).photoURI.path).delete()
        if (!status) {
            (fragment.context as MainActivity).showError("Не возможно удалить фото.")
            return
        }
        val taskItemPhotoId = (fragment.photosListAdapter.data[holder.adapterPosition] as ReportPhotosListModel.TaskItemPhoto).taskItem.id
        launch {
            val db = (fragment.activity!!.application as MyApplication).database
            val photoEntity = db.photosDao().getById(taskItemPhotoId)
            db.photosDao().delete(photoEntity)
        }

        fragment.photosListAdapter.data.removeAt(holder.adapterPosition)
        fragment.photosListAdapter.notifyItemRemoved(holder.adapterPosition)
    }

    fun onCloseClicked() {
        val nothingSelected = fragment.entrancesListAdapter.data.filter { it is ReportEntrancesListModel.Entrance }.any {
            (it as ReportEntrancesListModel.Entrance).selected == 0
        }
        //nothingSelected = nothingSelected or fragment.user_explanation_input.text.isBlank()

        if (nothingSelected) {
            (fragment.context as MainActivity).showError("Вы уверен что хотите закрыть адрес?", object : ErrorButtonsListener {
                override fun positiveListener() {
                    closeTaskItem()
                }

                override fun negativeListener() {}
            })
            return
        }
        closeTaskItem()
    }

    private fun closeTaskItem() {
        launch(UI) {
            val db = (fragment.activity!!.application as MyApplication).database
            withContext(CommonPool) {
                createOrUpdateTaskResult()
                db.taskItemDao().update(
                        db.taskItemDao().getById(fragment.taskItems[currentTask].id).let {
                            it.state = 1
                            it
                        }
                )
                db.taskDao().update(
                        db.taskDao().getById(fragment.tasks[currentTask].id).let {
                            if (it.state == TaskModel.EXAMINED) {
                                it.state = TaskModel.STARTED
                            }
                            it.updateTime = Date()
                            it
                        }
                )
            }

            (fragment.context as MainActivity).onBackPressed()
        }
    }
}
