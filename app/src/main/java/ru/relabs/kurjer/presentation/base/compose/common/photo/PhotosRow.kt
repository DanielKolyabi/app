package ru.relabs.kurjer.presentation.base.compose.common.photo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.relabs.kurjer.R
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorHasPhoto
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorRequiredPhoto

@Composable
fun<T> PhotosRow(
    photos: List<T>,
    mapper: (T) -> PhotoItemData,
    requiredPhoto: Boolean,
    hasPhoto: Boolean,
    modifier: Modifier = Modifier,
    onTakePhotoClicked: () -> Unit,
    onDeleteClicked: (photo: T) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    LazyRow(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        item {
            Icon(
                painter = painterResource(R.drawable.ic_entrance_photo),
                contentDescription = null,
                tint = if (requiredPhoto)
                    ColorRequiredPhoto
                else if (hasPhoto)
                    ColorHasPhoto
                else
                    Color.Black,
                modifier = Modifier
                    .padding(8.dp)
                    .size(48.dp)
                    .clickable(interactionSource = interactionSource, indication = null) { onTakePhotoClicked() }
                    .padding(4.dp)
            )
        }
        items(photos) {
            PhotoItem(mapper(it)) { onDeleteClicked(it) }
        }
    }
}