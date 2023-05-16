package ru.relabs.kurjer.domain.providers

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.media.MediaDrm
import android.os.Build
import android.os.Parcelable
import android.telephony.TelephonyManager
import kotlinx.parcelize.Parcelize
import ru.relabs.kurjer.utils.CustomLog
import java.util.*

@Parcelize
data class DeviceUniqueID(val id: String): Parcelable

class DeviceUniqueIdProvider(
    val application: Application
){
    private var imei: String? = null

    @SuppressLint("HardwareIds")
    fun get(): DeviceUniqueID {
        val ctxImei = imei
        if(ctxImei == null){
            val telephonyManager = application.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val newImei = try {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        val widevineUUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
                        val id =
                            MediaDrm(widevineUUID).getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
                        Base64.getEncoder().encodeToString(id)
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                        telephonyManager.getImei(0) ?: telephonyManager.getImei(1) ?: ""

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                        telephonyManager.getDeviceId(0) ?: (telephonyManager.getDeviceId(1) ?: "")

                    else ->
                        telephonyManager.deviceId
                }
            } catch (e: SecurityException) {
                CustomLog.writeToFile("IMEI: Stack \n" + CustomLog.getStacktraceAsString(e))
                ""
            }
            imei = newImei
            return DeviceUniqueID(newImei)
        }else{
            return DeviceUniqueID(ctxImei)
        }
    }
}