package ru.relabs.kurjer.uiOld.fragments

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.*
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.runtime.image.ImageProvider
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_yandex_map.*
import kotlinx.android.synthetic.main.fragment_yandex_map.view.*
import org.koin.android.ext.android.inject
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.Address
import ru.relabs.kurjer.domain.providers.LocationProvider
import ru.terrakok.cicerone.Router


class YandexMapFragment : Fragment() {
    private val router: Router by inject()
    private val locationProvider: LocationProvider by inject()
    private lateinit var userLocationLayer: UserLocationLayer
    private var addresses: List<AddressWithColor> = listOf()
    private var storages: List<StorageLocation> = listOf()
    private var exitInitialized: Boolean = false
    private val clickListener = MapObjectTapListener { obj, point ->
        val udata = obj.userData
        if (udata is Address) {
            onAddressClicked?.invoke(udata)
            if (!exitInitialized) {
                router.exit()
                exitInitialized = true
            }
            true
        }
        false
    }

    var onAddressClicked: ((Address) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            addresses = it.getParcelableArrayList("addresses") ?: emptyList()
            storages = it.getParcelableArrayList(ARG_STORAGES_KEY) ?: emptyList()
            if (addresses.size < 2) {
                savedCameraPosition = null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_yandex_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MapKitFactory.initialize(this.context)

        val point = locationProvider.lastReceivedLocation()?.let {
            Point(it.latitude, it.longitude)
        } ?: Point(0.0, 0.0)

        mapview.map.isRotateGesturesEnabled = false

        mapview.map.move(CameraPosition(point, 14f, 0f, 0f))
        userLocationLayer = mapview.map.userLocationLayer
        userLocationLayer.isEnabled = true

        val userLocation = locationProvider.lastReceivedLocation()

        my_position.setOnClickListener {
            mapview.map.move(
                userLocationLayer.cameraPosition()
                    ?: CameraPosition(Point(userLocation?.latitude ?: 0.0, userLocation?.longitude ?: 0.0), 14f, 0f, 0f)
            )
        }

        view.iv_menu.setOnClickListener {
            if (!exitInitialized) {
                router.exit()
                exitInitialized = true
            }
        }


        showStorages(storages)
        showAddresses(addresses)
        makeFocus(addresses)
    }

    private fun showStorages(storages: List<StorageLocation>) {
        mapview.map.mapObjects.forEach {
            if (it.userData == IconType.Storage) {
                mapview.map.mapObjects.remove(it)
            }
        }

        storages.forEach {
            mapview.map.mapObjects.addPlacemark(
                Point(it.storageLat.toDouble(), it.storageLong.toDouble()),
                ColoredIconProvider(mapview.context, Color.BLACK)
            ).apply {
                userData = IconType.Storage
            }

            mapview.map.mapObjects.addCircle(
                Circle(Point(it.storageLat.toDouble(), it.storageLong.toDouble()), 20f),
                Color.BLACK,
                2f,
                ColorUtils.setAlphaComponent(Color.BLACK, 80)
            ).apply {
                userData = IconType.Storage
            }
        }
    }


    private fun getCameraPosition(coloredAddresses: List<AddressWithColor>): CameraPosition {
        val userLocation = locationProvider.lastReceivedLocation()
        when {
            coloredAddresses.isEmpty() -> {
                return CameraPosition(Point(userLocation?.latitude ?: 0.0, userLocation?.longitude ?: 0.0), 14f, 0f, 0f)
            }
            coloredAddresses.size == 1 -> {
                val address = coloredAddresses.first().address
                return CameraPosition(
                    Point(address.lat.toDouble(), address.long.toDouble()),
                    14f, 0f, 0f
                )
            }
            else -> {
                val filtered = coloredAddresses.filter { it.address.lat != 0f && it.address.long != 0f }
                val minLat = filtered.minBy { it.address.lat }?.address?.lat?.toDouble()
                val maxLat = filtered.maxBy { it.address.lat }?.address?.lat?.toDouble()
                val minLong = filtered.minBy { it.address.long }?.address?.long?.toDouble()
                val maxLong = filtered.maxBy { it.address.long }?.address?.long?.toDouble()
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
        if (address.lat != 0f && address.long != 0f) {
            val ctx = context ?: return
            val point = Point(address.lat.toDouble(), address.long.toDouble())

            mapview.map.mapObjects
                .addPlacemark(point, ColoredIconProvider(ctx, coloredAddress.color))
                .apply {
                    userData = address
                }

            mapview.map.mapObjects.addCircle(
                Circle(point, 20f),
                R.color.colorPrimary,
                2f,
                ColorUtils.setAlphaComponent(coloredAddress.color, 125)
            ).apply {
                userData = address
                addTapListener(clickListener)
            }
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

        private const val ARG_STORAGES_KEY = "storages"

        var savedCameraPosition: CameraPosition? = null

        @JvmStatic
        fun newInstance(address: List<AddressWithColor>, storages: List<StorageLocation>) =
            YandexMapFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList("addresses", ArrayList(address))
                    putParcelableArrayList(ARG_STORAGES_KEY, ArrayList(storages))
                }
            }
    }

    @Parcelize
    data class StorageLocation(
        val storageLat: Float,
        val storageLong: Float
    ) : Parcelable

    @Parcelize
    data class AddressWithColor(
        val address: Address,
        val color: Int
    ) : Parcelable

    private enum class IconType {
        Address, Storage
    }
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

inline fun MapObjectCollection.forEach(crossinline block: (MapObject) -> Unit) {
    this.traverse(object : MapObjectVisitor {
        override fun onPolygonVisited(p0: PolygonMapObject) = block(p0)
        override fun onCircleVisited(p0: CircleMapObject) = block(p0)
        override fun onPolylineVisited(p0: PolylineMapObject) = block(p0)
        override fun onColoredPolylineVisited(p0: ColoredPolylineMapObject) = block(p0)
        override fun onPlacemarkVisited(p0: PlacemarkMapObject) = block(p0)
        override fun onCollectionVisitEnd(p0: MapObjectCollection) {}
        override fun onCollectionVisitStart(p0: MapObjectCollection): Boolean = true
    })
}