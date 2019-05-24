package ru.relabs.kurjer.ui.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.graphics.ColorUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.user_location.UserLocationLayer
import kotlinx.android.synthetic.main.fragment_yandex_map.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.application
import ru.relabs.kurjer.models.AddressModel


class YandexMapFragment : Fragment() {
    private lateinit var userLocationLayer: UserLocationLayer
    private var addresses: List<AddressModel> = listOf()


    var onAddressClicked: ((AddressModel) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            addresses = it.getParcelableArrayList("addresses") ?: listOf()
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
//        userLocationLayer = mapview.map.userLocationLayer
//        userLocationLayer.isEnabled = true
//        userLocationLayer.isHeadingEnabled = true

        my_position.setOnClickListener {
            mapview.map.move(
                    CameraPosition(
                            Point(application().currentLocation.lat, application().currentLocation.long),
                            14f, 0f, 0f
                    )
            )
        }


        showAddresses(addresses)
        makeFocus(addresses)
    }


    private fun getCameraPosition(addresses: List<AddressModel>): CameraPosition {
        when {
            addresses.isEmpty() -> {
                return CameraPosition(
                        Point(application().currentLocation.lat, application().currentLocation.long),
                        14f, 0f, 0f
                )
            }
            addresses.size == 1 -> {
                val address = addresses.first()
                return CameraPosition(
                        Point(address.lat, address.long),
                        14f, 0f, 0f
                )
            }
            else -> {
                val filtered = addresses.filter { it.lat != 0.0 && it.long != 0.0 }
                val minLat = filtered.minBy { it.lat }?.lat
                val maxLat = filtered.maxBy { it.lat }?.lat
                val minLong = filtered.minBy { it.long }?.long
                val maxLong = filtered.maxBy { it.long }?.long
                if (minLat == null || maxLat == null || minLong == null || maxLong == null) {
                    return getCameraPosition(listOfNotNull(addresses.firstOrNull()))

                }
                return mapview?.map?.cameraPosition(BoundingBox(Point(minLat, minLong), Point(maxLat, maxLong)))
                        ?: getCameraPosition(listOfNotNull(addresses.firstOrNull()))
            }
        }
    }

    fun makeFocus(addresses: List<AddressModel>) {

        mapview?.map?.move(getCameraPosition(addresses))
    }

    fun showAddresses(addresses: List<AddressModel>) {
        addresses.forEach(::showAddress)
    }

    private fun showAddress(address: AddressModel) {
        if (address.lat != 0.0 && address.long != 0.0) {
            val ctx = context ?: return
            val point = Point(address.lat, address.long)


            mapview.map.mapObjects
                    .addPlacemark(point)
                    .addTapListener { _, _ ->
                        onAddressClicked?.invoke(address)
                        activity?.onBackPressed()
                        true
                    }

            mapview.map.mapObjects.addCircle(
                    Circle(point, 50f),
                    R.color.colorPrimary,
                    2f,
                    ColorUtils.setAlphaComponent(resources.getColor(R.color.colorAccent), 125)
            )
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

        @JvmStatic
        fun newInstance(address: List<AddressModel>) =
                YandexMapFragment().apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList("addresses", ArrayList(address))
                    }
                }
    }
}
