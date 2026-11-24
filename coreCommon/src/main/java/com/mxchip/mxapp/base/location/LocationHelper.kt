package com.mxchip.mxapp.base.location

import android.Manifest
import android.content.Context
import android.location.Address
import android.location.Criteria
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import com.mxchip.mxapp.base.MXBusinessApplication
import java.util.Locale

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/11/23
 * @desc:
 */
object LocationHelper {

    private val locationManager by lazy {
       MXBusinessApplication.mxBusinessApp.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private val criteria by lazy {
        Criteria().apply {
            accuracy = Criteria.ACCURACY_FINE //高精度
            isAltitudeRequired = false //不要求海拔
            isBearingRequired = false //不要求方位
            isCostAllowed = true //允许有花费
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun location(block: (Location?) -> Unit) {
        val provider = locationManager.getBestProvider(criteria, true)
        if (provider == null) block.invoke(null)
        provider?.apply {
//            locationManager.requestLocationUpdates(this, 0L, 0f, locationListener)
            var location = locationManager.getLastKnownLocation(this)
            if (location != null) {
                block.invoke(location)
            } else {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (location != null) {
                    block.invoke(location)
                } else block.invoke(null)
            }
        }
    }

    fun getAddress(context: Context, lat: Double, long: Double): Address? {
        val gc = Geocoder(context, Locale.getDefault())
        var address: Address? = null
        return try {
            val locationList = gc.getFromLocation(lat, long, 1)
            locationList?.apply {
                address = this[0]
            }
            address
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}