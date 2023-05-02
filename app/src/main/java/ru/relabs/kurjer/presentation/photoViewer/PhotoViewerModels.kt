package ru.relabs.kurjer.presentation.photoViewer

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.providers.PathsProvider
import ru.relabs.kurjer.domain.repositories.TaskRepository
import ru.relabs.kurjer.presentation.base.tea.ElmEffect
import ru.relabs.kurjer.presentation.base.tea.ElmMessage
import ru.relabs.kurjer.presentation.base.tea.ErrorContext
import ru.relabs.kurjer.presentation.base.tea.ErrorContextImpl
import ru.relabs.kurjer.presentation.base.tea.RouterContext
import ru.relabs.kurjer.presentation.base.tea.RouterContextMainImpl
import java.io.File

data class PhotoViewerState(
    var currentPhoto: Int = 0,
    val photoPaths: List<String> = emptyList<String>()
) {
    val photo: String
        get() =
            if (currentPhoto < photoPaths.size)
                photoPaths[currentPhoto]
            else
                ""
}

class PhotoViewerContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent

typealias PhotoViewerMessage = ElmMessage<PhotoViewerContext, PhotoViewerState>
typealias PhotoViewerEffect = ElmEffect<PhotoViewerContext, PhotoViewerState>