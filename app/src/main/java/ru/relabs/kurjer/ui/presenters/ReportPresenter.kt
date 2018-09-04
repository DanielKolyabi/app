package ru.relabs.kurjer.ui.presenters

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.support.v7.widget.RecyclerView
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.MyApplication
import ru.relabs.kurjer.files.PathHelper.getTaskItemPhotoFile
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.persistence.entities.TaskItemPhotoEntity
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
        fillEntrancesAdapterData()
        fillPhotosAdapterData()

        (fragment.context as? MainActivity)?.changeTitle(fragment.taskItems[currentTask].address.name)
    }

    fun fillTasksAdapterData() {
        fragment.tasksListAdapter.data.addAll(fragment.tasks.mapIndexed { i, it ->
            ReportTasksListModel.TaskButton(it, i, i == 0)
        })
        fragment.tasksListAdapter.notifyDataSetChanged()
    }

    fun fillEntrancesAdapterData() {
        fragment.entrancesListAdapter.data.clear()
        if (fragment.tasks.size > 1) {
            fragment.setTaskListVisible(true)
        }
        fragment.entrancesListAdapter.data.addAll(fragment.taskItems[currentTask].entrances.map {
            ReportEntrancesListModel.Entrance(fragment.taskItems[currentTask], it, 0)
        })
        fragment.entrancesListAdapter.notifyDataSetChanged()
    }

    fun fillPhotosAdapterData() {
        launch(UI) {
            fragment.photosListAdapter.data.clear()
            val taskPhotos = withContext(CommonPool) {
                val db = (fragment.activity!!.application as MyApplication).database
                db.photosDao().getByTaskItemId(fragment.taskItems[currentTask].id).map {
                    it.toTaskItemPhotoModel(db)
                }
            }

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
        fragment.entrancesListAdapter.notifyItemChanged(holder.adapterPosition)
    }

    fun onBlankPhotoClicked(holder: RecyclerView.ViewHolder) {
        requestPhoto()
    }

    private fun requestPhoto(){

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
}
