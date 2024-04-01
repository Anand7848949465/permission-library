package com.iserveu.permission.multiplepermission

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.iserveu.permission.R
import com.iserveu.permission.utils.Util

object MultiPlePermission {
    class Builder : MultiplePermissionCallback {
        init {
            MyActivityResultCallback.setCallback(this)
        }

        companion object {
            private val TAG: String? = Builder::class.simpleName
        }

        private var mPermissionList = arrayOf<String>()
        private var needToAskPermission = false
        private lateinit var mOnUserAction: OnUserAction
        private lateinit var mContext: Context
        private lateinit var mMultiplePermissionLauncher: ActivityResultLauncher<Array<String>>
        private lateinit var mIntentLauncher: ActivityResultLauncher<Intent>


        fun permissionList(permissions: Array<String>) = apply {
            this.mPermissionList = permissions
        }

        fun context(context: Context) = apply {
            this.mContext = context
        }

        fun onCompletePermissionGranted(onUserAction: OnUserAction) =
            apply {
                this.mOnUserAction = onUserAction
            }

        fun multiplePermissionLauncher(multiplePermissionLauncher: ActivityResultLauncher<Array<String>>) =
            apply {
                this.mMultiplePermissionLauncher = multiplePermissionLauncher
            }

        fun intentResultLauncher(intentResultLauncher: ActivityResultLauncher<Intent>) =
            apply {
                this.mIntentLauncher = intentResultLauncher
            }

        fun build() = apply {
            require(::mContext.isInitialized) { "Context is mandatory" }
            require(::mIntentLauncher.isInitialized) { "Permission Launcher is mandatory" }
            require(::mOnUserAction.isInitialized) { "OnCompletePermissionGranted is mandatory" }
            require(::mMultiplePermissionLauncher.isInitialized) { "ActivityResultLauncher is mandatory" }
            checkPermissions()
        }

        private fun checkPermissions() {
            mPermissionList.forEach {
                if (ContextCompat.checkSelfPermission(
                        mContext,
                        it
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    needToAskPermission = true
                }
            }
            if (needToAskPermission) {
                mMultiplePermissionLauncher.launch(mPermissionList)
            } else {
                mOnUserAction.onAllPermissionGranted()
            }
        }

        /**
         * Displays a dialog prompting the user to grant location permission. If the user chooses to grant permission,
         * opens the application settings where the user can manually grant the required permission.
         * If the user cancels the dialog, finishes the activity.
         */
        private fun showSettingsDialog(mPermissionLauncher: ActivityResultLauncher<Intent>) {
            showAlert(mContext as Activity,
                mContext.getString(R.string.permission_required),
                mContext.getString(R.string.please_grant_permission),
                mContext.getString(R.string.settings),
                mContext.getString(R.string.cancel),
                { _: DialogInterface?, _: Int ->
                    // Open app settings
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", mContext.packageName, null)
                    intent.setData(uri)
                    mPermissionLauncher.launch(intent)
                },
                { _: DialogInterface?, _: Int -> mOnUserAction.onDeniedToGrantPermission() })
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
                dialog.setCanceledOnTouchOutside(false)
                dialog.show()
            } catch (e: ActivityNotFoundException) {
                Util.showLog(TAG, e.localizedMessage)
            } catch (e: IllegalArgumentException) {
                Util.showLog(TAG, e.localizedMessage)
            } catch (e: NullPointerException) {
                Util.showLog(TAG, e.localizedMessage)
            } catch (e: WindowManager.BadTokenException) {
                Util.showLog(TAG, e.localizedMessage)
            }
        }

        override fun handleMultiplePermissionCallBack(permissions: Map<String, Boolean>) {
            val grantedPermissions = permissions.filterValues {
                it
            }.keys
            // Check if any permission is permanently denied
            val isAnyPermissionPermanentlyDenied = permissions.any { permission ->
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    mContext as Activity,
                    permission.key
                ) && ContextCompat.checkSelfPermission(
                    mContext,
                    permission.key
                ) != PackageManager.PERMISSION_GRANTED
            }
            if (isAnyPermissionPermanentlyDenied) {
                showSettingsDialog(mIntentLauncher)
            } else if (permissions.size != grantedPermissions.size) {
                mMultiplePermissionLauncher.launch(mPermissionList)
            } else {
                mOnUserAction.onAllPermissionGranted()
            }
        }

        override fun handleActivityResultCallBack(activityResult: ActivityResult) {
            mMultiplePermissionLauncher.launch(mPermissionList)
        }
    }
}

interface OnUserAction {
    fun onAllPermissionGranted()
    fun onDeniedToGrantPermission()
}