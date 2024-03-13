Use Dependency

**implementation 'com.gitlab.isu_tech:utility:0.002'**__

/**
 * Launcher for requesting runtime permissions.
 */
 
```
private val callBack = MyActivityResultCallback()

private val mMultiplePermissionRequestLauncher: ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        callBack.onActivityResult(permissions)
    }

private val intentActivityResultLauncher: ActivityResultLauncher<Intent> =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        callBack.onActivityResult(result)
    }
```


/**
 * Multiple permission request.
 * You can ask the permission inside the onCreate function or on a button click
 */

    ```
MultiPlePermission
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
    .build()
```


/**
 * Bluetooth permission request with turn on bluetooth.
 * You can ask the permission inside the onCreate function or on a button click
 */

```
BluetoothAgent(
    mContext = context,
    permissionList= arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,  
        Manifest.permission.BLUETOOTH_SCAN),
    mMultiplePermissionRequestLauncher,
    intentActivityResultLauncher,
    callBack,
    object :ProceedOperationListener{
        override fun proceedWithUi() {
            Toast
                .makeText(context, "All Permission Granted",
                    Toast.LENGTH_SHORT)
                .show()
        }

        override fun onDeniedToGrantPermission() {
            Toast.makeText(context, "Cancelled",
                Toast.LENGTH_SHORT).show()
        }

        override fun onBluetoothNotSupported() {
            // Not Required
        }

    })   
    .checkBluetooth()
```



/**
 * Bluetooth permission request with turn on bluetooth.
 * You can ask the permission inside the onCreate function or on a button click
 */


```
LaunchedEffect(key1 = Unit ){
        LocationAgent(
        context ,
        mMultiplePermissionRequestLauncher,
        intentActivityResultLauncher,
        callBack,
        object :
            LocationUpdateListener {
            override fun onLocationUpdate(latLong: String?) {
                latLong?.let {
                    mViewModel.latLong.value = it
                }
            }

            override fun proceedWithUi() {
                mViewModel.isLocationFetched.value = true
            }
        },
    ).getLocation()
}
```









