package ru.relabs.kurjer.presentation.photoViewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.terrakok.cicerone.Router
import org.koin.android.ext.android.inject
import ru.relabs.kurjer.R
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.tea.defaultController

class PhotoViewerFragment : BaseFragment() {
    val controller = defaultController(PhotoViewerState(), PhotoViewerContext())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { PhotoViewerScreen(controller) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            val currentPhoto = savedInstanceState
                .getInt(SAVE_INDEX_KEY, -1)
                .takeIf { it != -1 }
                ?: return controller.start(PhotoViewerMessages.msgNavigateBack())
            val photoPaths = savedInstanceState.getStringArrayList(SAVE_PHOTO_PATHS_KEY)
                ?.takeIf { it.isNotEmpty() }
                ?: return controller.start(PhotoViewerMessages.msgNavigateBack())
            controller.start(PhotoViewerMessages.msgInit(currentPhoto, photoPaths))
        } else {
            val currentPhoto = 0
            val photoPaths = arguments
                ?.getStringArrayList(ARG_PHOTO_PATH_KEY)
                ?.takeIf { it.isNotEmpty() }
                ?: return controller.start(PhotoViewerMessages.msgNavigateBack())
            controller.start(PhotoViewerMessages.msgInit(currentPhoto, photoPaths))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_INDEX_KEY, controller.currentState.currentPhoto)
        outState.putStringArrayList(SAVE_PHOTO_PATHS_KEY, ArrayList(controller.currentState.photoPaths))
    }

    companion object {
        private const val ARG_PHOTO_PATH_KEY = "photo_paths"
        private const val SAVE_INDEX_KEY = "photo_index"
        private const val SAVE_PHOTO_PATHS_KEY = "photo_paths"
        fun newInstance(photoPaths: List<String>) = PhotoViewerFragment().apply {
            arguments = Bundle().apply {
                putStringArrayList(ARG_PHOTO_PATH_KEY, ArrayList(photoPaths))
            }
        }
    }
}