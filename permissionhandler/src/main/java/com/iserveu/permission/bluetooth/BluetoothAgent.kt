package com.iserveu.permission.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import com.iserveu.permission.multiplepermission.MultiPlePermission
import com.iserveu.permission.multiplepermission.MultiplePermissionCallback
import com.iserveu.permission.multiplepermission.MyActivityResultCallback
import com.iserveu.permission.multiplepermission.OnUserAction
import com.iserveu.permission.utils.Util

class BluetoothAgent(
    private val mContext: Context,
    private val permissionList: Array<String>,
    private val mMultiplePermissionLauncher: ActivityResultLauncher<Array<String>>,
    private val mIntentLauncher: ActivityResultLauncher<Intent>,
    private val proceedOperationListener: ProceedOperationListener,
) : MultiplePermissionCallback {

    private val bluetoothAdapter: BluetoothAdapter? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val bluetoothManager: BluetoothManager? = mContext.getSystemService(
                BluetoothManager::class.java
            )
            bluetoothManager?.adapter
        } else {
            BluetoothAdapter.getDefaultAdapter()
        }

    fun checkBluetooth() {
        bluetoothAdapter?.let { btAdapter ->
            Log.d(
                "CHECKFIRST", Util.checkAllPermissionGranted(
                    mContext,
                    permissionList
                ).toString()
            )
            if (Util.checkAllPermissionGranted(
                    mContext,
                    permissionList
                ) && btAdapter.isEnabled
            ) {
                proceedOperationListener.proceedWithUi()
                return
            }
            if (!Util.checkAllPermissionGranted(
                    mContext,
                    permissionList
                )
            ) {
                MultiPlePermission
                    .Builder()
                    .context(mContext)
                    .permissionList(
                        permissionList
                    )
                    .onCompletePermissionGranted(object : OnUserAction {
                        override fun onAllPermissionGranted() {
                            checkBluetooth()
                        }

                        override fun onDeniedToGrantPermission() {
                            proceedOperationListener.onDeniedToGrantPermission()
                        }
                    })
                    .multiplePermissionLauncher(mMultiplePermissionLauncher)
                    .intentResultLauncher(mIntentLauncher)
                    .build()
            } else {
                MyActivityResultCallback.setCallback(this)
                val turnOn = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                this.mIntentLauncher.launch(turnOn)
            }
        } ?: {
            proceedOperationListener.onBluetoothNotSupported()
        }

    }

    override fun handleMultiplePermissionCallBack(permissions: Map<String, Boolean>) {
        // Not Required
    }

    override fun handleActivityResultCallBack(activityResult: ActivityResult) {
        if (activityResult.resultCode == Activity.RESULT_OK) {
            checkBluetooth()
        }
    }
}