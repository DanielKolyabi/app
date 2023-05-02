package ru.relabs.kurjer.presentation.photoViewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import ru.relabs.kurjer.presentation.base.compose.ElmScaffold
import ru.relabs.kurjer.presentation.base.tea.ElmController

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun PhotoViewerScreen(controller: ElmController<PhotoViewerContext, PhotoViewerState>) = ElmScaffold(controller) {
    val currentPhoto by watchAsState { it.photo }

    GlideImage(
        model = currentPhoto,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .clickable { sendMessage(PhotoViewerMessages.msgImageClicked()) }
    )

}