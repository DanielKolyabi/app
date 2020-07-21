package ru.relabs.kurjer.ui.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.runtime.image.ImageProvider
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_yandex_map.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.utils.application
import ru.relabs.kurjer.models.AddressModel


class YandexMapFragment : Fragment() {
    private lateinit var userLocationLayer: UserLocationLayer
    private var addresses: List<AddressWithColor> = listOf()


    var onAddressClicked: ((AddressModel) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            addresses = it.getParcelableArrayList("addresses") ?: listOf()
            if (addresses.size < 2) {
                savedCameraPosition = null
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_yandex_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MapKitFactory.initialize(this.context)

        val point = Point(application().currentLocation.lat, application().currentLocation.long)
        mapview.map.isRotateGesturesEnabled = false

        mapview.map.move(
                CameraPosition(point, 14f, 0f, 0f)
        )
        userLocationLayer = mapview.map.userLocationLayer
        userLocationLayer.isEnabled = true
//        userLocationLayer.isHeadingEnabled = true

        my_position.setOnClickListener {
            mapview.map.move(
                    userLocationLayer.cameraPosition() ?: CameraPosition(
                            Point(application().currentLocation.lat, application().currentLocation.long),
                            14f, 0f, 0f
                    )
            )
        }


        showAddresses(addresses)
        makeFocus(addresses)
    }


    private fun getCameraPosition(coloredAddresses: List<AddressWithColor>): CameraPosition {
        when {
            coloredAddresses.isEmpty() -> {
                return CameraPosition(
                        Point(application().currentLocation.lat, application().currentLocation.long),
                        14f, 0f, 0f
                )
            }
            coloredAddresses.size == 1 -> {
                val address = coloredAddresses.first().address
                return CameraPosition(
                        Point(address.lat, address.long),
                        14f, 0f, 0f
                )
            }
            else -> {
                val filtered = coloredAddresses.filter { it.address.lat != 0.0 && it.address.long != 0.0 }
                val minLat = filtered.minBy { it.address.lat }?.address?.lat
                val maxLat = filtered.maxBy { it.address.lat }?.address?.lat
                val minLong = filtered.minBy { it.address.long }?.address?.long
                val maxLong = filtered.maxBy { it.address.long }?.address?.long
                if (minLat == null || maxLat == null || minLong == null || maxLong == null) {
                    return getCameraPosition(listOfNotNull(coloredAddresses.firstOrNull()))

                }
                return mapview?.map?.cameraPosition(BoundingBox(Point(minLat, minLong), Point(maxLat, maxLong)))
                        ?: getCameraPosition(listOfNotNull(coloredAddresses.firstOrNull()))
            }
        }
    }

    fun makeFocus(addresses: List<AddressWithColor>) {
        mapview?.map?.move(savedCameraPosition ?: getCameraPosition(addresses))
    }

    fun showAddresses(addresses: List<AddressWithColor>) {
        addresses.forEach(::showAddress)
    }

    private fun showAddress(coloredAddress: AddressWithColor) {
        val address = coloredAddress.address
        if (address.lat != 0.0 && address.long != 0.0) {
            val ctx = context ?: return
            val point = Point(address.lat, address.long)


            mapview.map.mapObjects
                    .addPlacemark(point, ColoredIconProvider(ctx, coloredAddress.color))
                    .addTapListener { _, _ ->
                        onAddressClicked?.invoke(address)
                        activity?.onBackPressed()
                        true
                    }

            mapview.map.mapObjects.addCircle(
                    Circle(point, 7.5f),
                    R.color.colorPrimary,
                    2f,
                    ColorUtils.setAlphaComponent(coloredAddress.color, 125)
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (addresses.size > 1) {
            savedCameraPosition = mapview?.map?.cameraPosition
        }
    }

    override fun onStop() {
        super.onStop()
        MapKitFactory.getInstance().onStop()
        mapview.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapview.onStart()
    }


    companion object {

        var savedCameraPosition: CameraPosition? = null

        @JvmStatic
        fun newInstance(address: List<AddressWithColor>) =
                YandexMapFragment().apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList("addresses", ArrayList(address))
                    }
                }
    }

    @Parcelize
    data class AddressWithColor(
            val address: AddressModel,
            val color: Int
    ) : Parcelable
}


class ColoredIconProvider(val context: Context, val color: Int) : ImageProvider() {
    override fun getId(): String {
        return "colored:${color}"
    }

    override fun getImage(): Bitmap {
        val drawable = context.resources.getDrawable(R.drawable.house_map_icon)
        val filter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
        drawable.colorFilter = filter
        val bmp = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }
}
