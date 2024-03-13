package com.iserveu.permission.location

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.view.Window
import android.view.WindowManager.BadTokenException
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.iserveu.permission.R
import com.iserveu.permission.multiplepermission.MultiPlePermission
import com.iserveu.permission.multiplepermission.MultiplePermissionCallback
import com.iserveu.permission.multiplepermission.MyActivityResultCallback
import com.iserveu.permission.multiplepermission.OnUserAction
import com.iserveu.permission.utils.Util.checkLocationPermission
import com.iserveu.permission.utils.Util.showLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationAgent(
    private val mContext: Context,
    private val mMultiplePermissionLauncher: ActivityResultLauncher<Array<String>>,
    private val mIntentLauncher: ActivityResultLauncher<Intent>,
    private val callback: MyActivityResultCallback,
    private val mLocationUpdateListener: LocationUpdateListener,
) : MultiplePermissionCallback {
    private var mLocationManager: LocationManager? = null
    private val timeOutMilliSeconds = 3000 // Timeout duration in milliseconds
    private var mIsLocationReceived = false // Flag to track if location is received
    private var mIsLocationDialogShowing = false
    private var mEditor: SharedPreferences.Editor? = null
    private var mLatLongSharedPreferences: SharedPreferences? = null
    private var mPermissionDeniedCount = 0

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            getLatLongAndSaveInSharedPreference(location)
        }

        override fun onProviderDisabled(provider: String) {
            super.onProviderDisabled(provider)
            if (!mIsLocationDialogShowing) {
                mIsLocationReceived = false
                mIsLocationDialogShowing =
                    true // Set flag to true to indicate that the dialogue is showing
                handleGps()
            }
        }
    }


    companion object {
        private val TAG: String? = LocationAgent::class.simpleName
    }

    init {
        callback.setCallback(this)
        val mSharedPreference: SharedPreferences =
            mContext.getSharedPreferences(mContext.getString(R.string.unified_aeps), MODE_PRIVATE)
        mLatLongSharedPreferences =
            mContext.getSharedPreferences(
                mContext.getString(R.string.location_preference),
                MODE_PRIVATE
            )
        mEditor = mSharedPreference.edit()
        mPermissionDeniedCount =
            mSharedPreference.getInt(mContext.getString(R.string.denied_count), 0)
        mLocationManager = mContext.getSystemService(LOCATION_SERVICE) as LocationManager
    }

    fun getLocation() {
        val isGpsEnabled: Boolean =
            mLocationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        if (isGpsEnabled && checkLocationPermission(mContext)) {
            mLocationUpdateListener.proceedWithUi()
        }
        if (checkLocationPermission(mContext)) {
            mLatLongSharedPreferences?.let {
                if (it.contains(mContext.getString(R.string.latlong))) {
                    val lastLatLong =
                        it.getString(mContext.getString(R.string.latlong), "0.0,0.0")
                    mLocationUpdateListener.onLocationUpdate(lastLatLong)
                }
            }
            handleGps()
        } /*else if (mPermissionDeniedCount == 2) {
            showSettingsDialog()
        }*/ else {
            MultiPlePermission
                .Builder()
                .context(mContext)
                .permissionList(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                .onCompletePermissionGranted(object : OnUserAction {
                    override fun onAllPermissionGranted() {
                        getLocation()
                    }

                    override fun onDeniedToGrantPermission() {
                        mLocationUpdateListener.onDeniedToGrantPermission()
                    }
                })
                .multiplePermissionLauncher(mMultiplePermissionLauncher)
                .intentResultLauncher(mIntentLauncher)
                .callBack(callback)
                .build()
        }
    }

    /**
     * Displays a dialog prompting the user to grant location permission. If the user chooses to grant permission,
     * opens the application settings where the user can manually grant the required permission.
     * If the user cancels the dialog, finishes the activity.
     */
    private fun showSettingsDialog() {
        showAlert(mContext as Activity,
            mContext.getString(R.string.location_permission_required),
            mContext.getString(R.string.please_grant_location_permission),
            mContext.getString(R.string.settings),
            mContext.getString(R.string.cancel),
            { _: DialogInterface?, _: Int ->
                // Open app settings
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", mContext.getPackageName(), null)
                intent.setData(uri)
                mIntentLauncher.launch(intent)
            },
            { _: DialogInterface?, _: Int -> mLocationUpdateListener.onDeniedToGrantPermission() })
    }

    /**
     * Handles GPS functionality. Checks if GPS is enabled on the device.
     * If GPS is enabled, proceeds to retrieve latitude and longitude coordinates.
     * If GPS is not enabled, displays a dialog prompting the user to enable GPS.
     * If the user chooses to enable GPS, opens the device settings where the user can enable GPS.
     * If the user cancels the dialog, finishes the activity.
     */
    private fun handleGps() {
        val isGpsEnabled: Boolean =
            mLocationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        if (isGpsEnabled) {
            getLatitudeLongitude()
        } else {
            showAlert(mContext as Activity, mContext.getString(R.string.alert),
                mContext.getString(R.string.gps_is_not_enabled_do_you_want_to_go_to_settings_menu),
                mContext.getString(R.string.settings),
                mContext.getString(R.string.cancel),
                { _: DialogInterface?, _: Int ->
                    val intent =
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    mIntentLauncher.launch(intent)
                    mIsLocationDialogShowing = false
                }, { _: DialogInterface?, _: Int ->
                    mIsLocationDialogShowing = false
                    mLocationUpdateListener.onDeniedToTurnOnLocation()
                }
            )
        }
    }

    /**
     * Displays a dialog with customizable title, message, positive and negative buttons,
     * and corresponding click listeners.
     *
     * @param showAlertContext      The activity context to display the dialog.
     * @param title                 The title of the dialog.
     * @param msg                   The message content of the dialog.
     * @param positiveText          The text to display on the positive button.
     * @param negativeText          The text to display on the negative button.
     * @param positiveClickListener The click listener for the positive button.
     * @param negativeClickListener The click listener for the negative button.
     */
    private fun showAlert(
        showAlertContext: Activity?,
        title: String?, msg: String?,
        positiveText: String?, negativeText: String?,
        positiveClickListener: DialogInterface.OnClickListener?,
        negativeClickListener: DialogInterface.OnClickListener?,
    ) {
        try {
            if (showAlertContext?.isDestroyed == false) {
                val dialogTitle = title ?: mContext.getString(R.string.alert)
                val dialogMessage = msg ?: ""
                val dialogPositiveButtonText = positiveText ?: mContext.getString(R.string.ok)
                val builder: AlertDialog.Builder = AlertDialog.Builder(showAlertContext)
                builder.setTitle(dialogTitle)
                builder.setMessage(dialogMessage)
                builder.setPositiveButton(dialogPositiveButtonText) { dialog, which ->
                    dialog.dismiss()
                    positiveClickListener?.onClick(dialog, which)
                }
                if (!negativeText.isNullOrEmpty()) {
                    builder.setNegativeButton(negativeText) { dialog, which ->
                        dialog.dismiss()
                        negativeClickListener?.onClick(dialog, which)
                    }
                }
                val dialog: AlertDialog = builder.create()
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setCancelable(false)
                dialog.show()
            }
        } catch (e: ActivityNotFoundException) {
            showLog(TAG, e.localizedMessage)
        } catch (e: IllegalArgumentException) {
            showLog(TAG, e.localizedMessage)
        } catch (e: NullPointerException) {
            showLog(TAG, e.localizedMessage)
        } catch (e: BadTokenException) {
            showLog(TAG, e.localizedMessage)
        }
    }

    /**
     * Retrieves the latitude and longitude from the provided Location object and saves them in shared preferences.
     *
     * @param location The Location object containing the latitude and longitude information.
     */
    private fun getLatLongAndSaveInSharedPreference(location: Location?) {
        location?.let {
            mIsLocationReceived = true
            val latitude = it.latitude
            val longitude = it.longitude
            mLocationUpdateListener.onLocationUpdate("$latitude,$longitude")
            mLatLongSharedPreferences?.edit()?.apply {
                putString(mContext.getString(R.string.latlong), "$latitude,$longitude")
                apply()
            }
        }
    }

    /**
     * Retrieves the latitude and longitude coordinates.
     * If the necessary permissions are granted, it requests location updates from
     * the LocationManager. If no location is received within a specified timeout period,
     * it resorts to using the FusedLocationProviderClient to retrieve the location.
     */
    private fun getLatitudeLongitude() {
        // Initialize the FusedLocationProviderClient
        val fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(mContext)

        // Check if the necessary location permissions are granted
        if (ContextCompat.checkSelfPermission(
                mContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If permissions are not granted, request them
            getLocation()
        } else {
            // Request location updates from LocationManager
            mLocationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                0f,
                locationListener
            )
            mLocationManager?.requestLocationUpdates(
                LocationManager.PASSIVE_PROVIDER,
                1000,
                0f,
                locationListener
            )
            mLocationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000,
                0f,
                locationListener
            )
            // Set up a handler to implement timeout
            CoroutineScope(Dispatchers.Main).launch {
                delay(timeOutMilliSeconds.toLong())
                if (!mIsLocationReceived) {
                    // If location is not received within the timeout period,
                    // request location from FusedLocationProviderClient
                    fusedLocationClient.getCurrentLocation(
                        LocationRequest.PRIORITY_HIGH_ACCURACY,
                        CancellationTokenSource().token
                    ).addOnCompleteListener { task: Task<Location?>? ->
                        this@LocationAgent.handleLocationListenerTask(
                            task
                        )
                    }
                }
            }
        }
    }

    /**
     * Handles the completion task of fetching the location.
     * Updates the location fetching loader status to false.
     * If the task is successful, extracts the latitude and longitude coordinates
     * from the task result and notifies the location update listener.
     * If the task encounters an exception, notifies the location update listener
     * about the error and logs the exception message.
     *
     * @param task The task representing the location fetching operation.
     */
    private fun handleLocationListenerTask(task: Task<Location?>?) {
        try {
            task?.let {
                if (it.isSuccessful) {
                    getLatLongAndSaveInSharedPreference(it.result)
                } else if (it.exception != null) {
                    showLog(
                        "", if (it.exception!!.localizedMessage != null) it.exception!!
                            .localizedMessage else ""
                    )
                }
            }
        } catch (e: java.lang.NullPointerException) {
            showLog("", if (e.localizedMessage != null) e.localizedMessage else "")
        }
    }

    override fun handleMultiplePermissionCallBack(permissions: Map<String, Boolean>) {
        // Not Required
    }

    override fun handleActivityResultCallBack(activityResult: ActivityResult) {
        getLocation()
    }

}