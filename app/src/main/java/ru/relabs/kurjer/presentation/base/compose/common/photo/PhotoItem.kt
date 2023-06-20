package ru.relabs.kurjer.presentation.base.compose.common.photo

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import ru.relabs.kurjer.R
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorBackgroundGray

data class PhotoItemData(val entranceText: String?, val uri: Uri)

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun PhotoItem(data: PhotoItemData, modifier: Modifier = Modifier, onIconClicked: () -> Unit) {
    Box(
        modifier = modifier
            .size(64.dp)
            .padding(8.dp)
    ) {
        GlideImage(
            model = data.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
        )
        Icon(
            painter = painterResource(R.drawable.ic_cancel_black_24dp),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable { onIconClicked() }
                .padding(top = 2.dp, end = 2.dp)
                .size(24.dp)
        )
        if (!data.entranceText.isNullOrEmpty())
            Text(
                text = data.entranceText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 3.dp, bottom = 2.dp)
                    .size(20.dp)
                    .background(color = ColorBackgroundGray, shape = CircleShape)
            )
    }
}