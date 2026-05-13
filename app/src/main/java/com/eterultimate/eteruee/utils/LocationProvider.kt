package com.eterultimate.eteruee.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

/**
 * 位置信息提供者
 * 
 * 用于获取设备当前位置信息，支持精确位置和粗略位置
 */
object LocationProvider {
    
    /**
     * 获取当前位置信息
     * 
     * @param context Android Context
     * @return 位置信息字符串，格式为 "纬度, 经度" 或错误信息
     */
    fun getCurrentLocation(context: Context): String {
        return try {
            // 检查权限
            if (!hasLocationPermission(context)) {
                return "位置权限未授予"
            }
            
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            // 尝试获取最后已知位置（从 GPS 或网络提供者）
            val location = getLastKnownLocation(locationManager)
            
            if (location != null) {
                formatLocation(location)
            } else {
                "无法获取位置信息"
            }
        } catch (e: SecurityException) {
            "位置权限被拒绝"
        } catch (e: Exception) {
            "无法获取位置: ${e.message}"
        }
    }
    
    /**
     * 检查是否有位置权限
     */
    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 获取最后已知位置
     */
    private fun getLastKnownLocation(locationManager: LocationManager): Location? {
        // 优先使用 GPS
        val gpsLocation = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: SecurityException) {
            null
        }
        
        if (gpsLocation != null) {
            return gpsLocation
        }
        
        // 其次使用网络定位
        val networkLocation = try {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            null
        }
        
        return networkLocation
    }
    
    /**
     * 格式化位置信息
     */
    private fun formatLocation(location: Location): String {
        val latitude = location.latitude
        val longitude = location.longitude
        
        // 保留6位小数，约1米精度
        return String.format("%.6f, %.6f", latitude, longitude)
    }
}
