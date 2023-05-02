package ru.relabs.kurjer.presentation.photoViewer

object PhotoViewerEffects {
    fun effectNavigateBack(): PhotoViewerEffect = { c, s ->
        c.router.exit()
    }
}