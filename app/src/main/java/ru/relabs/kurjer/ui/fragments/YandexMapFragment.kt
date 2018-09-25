package ru.relabs.kurjer.ui.fragments

import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.graphics.ColorUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import kotlinx.android.synthetic.main.fragment_yandex_map.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.application
import ru.relabs.kurjer.models.AddressModel


class YandexMapFragment : Fragment(), UserLocationObjectListener {
    private lateinit var userLocationLayer: UserLocationLayer
    private lateinit var address: AddressModel

    override fun onObjectUpdated(userLocationView: UserLocationView, p1: ObjectEvent?) {

        userLocationLayer.setAnchor(
                PointF((mapview.width * 0.5).toFloat(), (mapview.height * 0.5).toFloat()),
                PointF((mapview.width * 0.5).toFloat(), (mapview.height * 0.83).toFloat()))

        userLocationView.pin.setIcon(ImageProvider.fromResource(
                context, R.drawable.ic_arrow))
        userLocationView.arrow.setIcon(ImageProvider.fromResource(
                context, R.drawable.ic_arrow))
        userLocationView.accuracyCircle.fillColor = Color.argb(125, 255, 63, 81)
    }

    override fun onObjectRemoved(p0: UserLocationView?) {}

    override fun onObjectAdded(p0: UserLocationView?) {}

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
        userLocationLayer.setObjectListener(this)
    }

    override fun onStop() {
        super.onStop()
        mapview.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onStart() {
        super.onStart()
        mapview.onStart()
        MapKitFactory.getInstance().onStart()
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
