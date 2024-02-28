package com.iserveu.permissionhandler

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.iserveu.permission.multiplepermission.MultiPlePermission
import com.iserveu.permission.multiplepermission.MyActivityResultCallback
import com.iserveu.permission.multiplepermission.OnCompletePermissionGranted
import com.iserveu.permissionhandler.ui.theme.PermissionHandlerTheme


class MainActivity : ComponentActivity() {
    private val callBack = MyActivityResultCallback()

    /**
     * Launcher for requesting runtime permissions.
     */
    private val mMultiplePermissionRequestLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            callBack.onActivityResult(permissions)
        }
    private val intentActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            callBack.onActivityResult(result)
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
                    MultiPlePermission
                        .Builder()
                        .context(context)
                        .permissionList(
                            permissionList
                        )
                        .onCompletePermissionGranted(object : OnCompletePermissionGranted {
                            override fun onComplete() {
                                Toast
                                    .makeText(context, "All Permission Granted", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        })
                        .multiplePermissionLauncher(mMultiplePermissionRequestLauncher)
                        .intentResultLauncher(intentActivityResultLauncher)
                        .callBack(callBack)
                        .build()
                }
            }
        }
    }
}
