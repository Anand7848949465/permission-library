package com.iserveu.permissionhandler

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.iserveu.permission.bluetooth.BluetoothAgent
import com.iserveu.permission.bluetooth.ProceedOperationListener
import com.iserveu.permission.location.LocationAgent
import com.iserveu.permission.location.LocationUpdateListener
import com.iserveu.permission.multiplepermission.MyActivityResultCallback
import com.iserveu.permissionhandler.ui.theme.PermissionHandlerTheme
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {

    /**
     * Launcher for requesting runtime permissions.
     */
    private val mMultiplePermissionRequestLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            MyActivityResultCallback.onActivityResult(permissions)
        }
    private val intentActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            MyActivityResultCallback.onActivityResult(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PermissionHandlerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val permissionList = arrayOf(
                        "android.permission.ACCESS_FINE_LOCATION",
                        "android.permission.ACCESS_COARSE_LOCATION",
                        "android.permission.CAMERA",
                        "android.permission.READ_MEDIA_AUDIO",
                        "android.permission.READ_MEDIA_IMAGES",
                        "android.permission.READ_MEDIA_VIDEO"
                    )
                    /*MultiPlePermission
                        .Builder()
                        .context(context)
                        .permissionList(
                            permissionList
                        )
                        .onCompletePermissionGranted(object : OnUserAction {
                            override fun onAllPermissionGranted() {
                                Toast
                                    .makeText(context, "All Permission Granted", Toast.LENGTH_SHORT)
                                    .show()
                            }

                            override fun onDeniedToGrantPermission() {
                                Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
                            }
                        })
                        .multiplePermissionLauncher(mMultiplePermissionRequestLauncher)
                        .intentResultLauncher(intentActivityResultLauncher)
                        .callBack(callBack)
                        .build()*/
                    val showAlert = rememberSaveable {
                        mutableStateOf(false)
                    }
                    if (showAlert.value) {
                        Toast.makeText(context, "Denied", Toast.LENGTH_SHORT).show()
                    }

                    LaunchedEffect(key1 = Unit) {
                        LocationAgent(
                            context,
                            mMultiplePermissionRequestLauncher,
                            intentActivityResultLauncher,
                            object :
                                LocationUpdateListener {
                                override fun onDeniedToGrantPermission() {
                                    showAlert.value = true
                                }

                                override fun onDeniedToTurnOnLocation() {
                                    showAlert.value = true
                                }

                                override fun onLocationUpdate(latLong: String?) {
                                    latLong?.let {
                                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun proceedWithUi() {
                                }
                            },
                        ).getLocation()
                    }


                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Turn On Bluetooth",
                            fontSize = 18.sp,
                            modifier = Modifier.clickable {
                                BluetoothAgent(
                                    mContext = context,
                                    permissionList = arrayOf(
                                        Manifest.permission.BLUETOOTH_CONNECT,
                                        Manifest.permission.BLUETOOTH_SCAN
                                    ),
                                    mMultiplePermissionRequestLauncher,
                                    intentActivityResultLauncher,
                                    object : ProceedOperationListener {
                                        override fun proceedWithUi() {
                                            Toast
                                                .makeText(
                                                    context,
                                                    "All Permission Granted",
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        }

                                        override fun onDeniedToGrantPermission() {
                                            Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT)
                                                .show()
                                        }

                                        override fun onBluetoothNotSupported() {
                                            // Not Required
                                        }

                                    }
                                )
                                    .checkBluetooth()
                            }
                        )
                    }
                }
            }
        }
    }
}
