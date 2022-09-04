package ru.relabs.kurjer.presentation.photoViewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import org.koin.android.ext.android.inject
import ru.relabs.kurjer.R
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.terrakok.cicerone.Router

class PhotoViewerFragment : BaseFragment() {
    val router: Router by inject()
    var currentPhoto = 0
    var photoPaths = emptyList<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_photo_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            currentPhoto = savedInstanceState
                .getInt(SAVE_INDEX_KEY, -1)
                .takeIf { it != -1 }
                ?: return router.exit()
            photoPaths = savedInstanceState.getStringArrayList(SAVE_PHOTO_PATHS_KEY)
                ?.takeIf { it.isNotEmpty() }
                ?: return router.exit()
        } else {
            currentPhoto = 0
            photoPaths = arguments
                ?.getStringArrayList(ARG_PHOTO_PATH_KEY)
                ?.takeIf { it.isNotEmpty() }
                ?: return router.exit()
        }

        loadImage(view as ImageView, photoPaths[currentPhoto])

        view.setOnClickListener {
            currentPhoto++
            if (currentPhoto >= photoPaths.size) {
                router.exit()
            } else {
                loadImage(view, photoPaths[currentPhoto])
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_INDEX_KEY, currentPhoto)
        outState.putStringArrayList(SAVE_PHOTO_PATHS_KEY, ArrayList(photoPaths))
    }

    private fun loadImage(view: ImageView, path: String) {
        Glide
            .with(requireContext())
            .load(path)
            .into(view)
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