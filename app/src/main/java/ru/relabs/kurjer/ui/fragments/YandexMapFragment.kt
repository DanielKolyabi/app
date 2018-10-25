package ru.relabs.kurjer.ui.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.graphics.ColorUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.yandex.mapkit.MapKitFactory
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
    private lateinit var address: AddressModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            address = it.getParcelable("address")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_yandex_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MapKitFactory.initialize(this.context)
        var point = Point(0.0, 0.0)
        if (application() != null) {
            point = Point(application()!!.currentLocation.lat, application()!!.currentLocation.long)
        }
        mapview.map.isRotateGesturesEnabled = false
        if (address.lat != 0.0 && address.long != 0.0) {
            point = Point(address.lat, address.long)

            mapview.map.mapObjects.addPlacemark(Point(address.lat, address.long))
            mapview.map.mapObjects.addCircle(
                    Circle(point, 100f),
                    R.color.colorPrimary,
                    2f,
                    ColorUtils.setAlphaComponent(resources.getColor(R.color.colorAccent), 125)
            )
        }
        mapview.map.move(
                CameraPosition(point, 14f, 0f, 0f)
        )
        userLocationLayer = mapview.map.userLocationLayer
        userLocationLayer.isEnabled = true
        userLocationLayer.isHeadingEnabled = true

        my_position.setOnClickListener {
            if (application() != null) {
                val point = Point(application()!!.currentLocation.lat, application()!!.currentLocation.long)
                mapview.map.move(
                        CameraPosition(point, 14f, 0f, 0f)
                )
            }
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
        fun newInstance(address: AddressModel) =
                YandexMapFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("address", address)
                    }
                }
    }
}
