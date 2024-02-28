package com.iserveu.permission.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.iserveu.permission.BuildConfig

object Util {
    fun showLog(tag: String?, message: String?) {
        val isShowLog: Boolean = BuildConfig.DEBUG
        if (isShowLog) {
            Log.d(tag, message!!)
        }
    }

    fun checkLocationPermission(context: Context): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

}