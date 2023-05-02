package ru.relabs.kurjer.presentation.photoViewer

import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.presentation.base.tea.msgState

object PhotoViewerMessages {
    fun msgInit(currentPhoto: Int, photoPaths: ArrayList<String>): PhotoViewerMessage =
        msgState {
            it.copy(currentPhoto = currentPhoto, photoPaths = photoPaths)
        }

    fun msgNavigateBack(): PhotoViewerMessage =
        msgEffect(PhotoViewerEffects.effectNavigateBack())

    fun msgImageClicked(): PhotoViewerMessage =
        msgEffects(
            { it.copy(currentPhoto = it.currentPhoto.inc()) },
            {
                if (it.currentPhoto >= it.photoPaths.size)
                    listOf(PhotoViewerEffects.effectNavigateBack())
                else
                    listOf()
            }
        )


}