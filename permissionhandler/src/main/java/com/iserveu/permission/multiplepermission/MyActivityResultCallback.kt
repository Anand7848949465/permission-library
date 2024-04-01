package com.iserveu.permission.multiplepermission

import androidx.activity.result.ActivityResult
import com.iserveu.permission.utils.Util

object MyActivityResultCallback {
    private var multiplePermissionCallback: MultiplePermissionCallback? = null


    fun setCallback(multiplePermissionCallBack: MultiplePermissionCallback) {
        multiplePermissionCallback = multiplePermissionCallBack
    }

    fun onActivityResult(permissions: Any) {
        if (permissions is Map<*, *>) {
           multiplePermissionCallback?.handleMultiplePermissionCallBack(permissions as Map<String, Boolean>)
        } else if (permissions is ActivityResult) {
            try {
                multiplePermissionCallback?.handleActivityResultCallBack(permissions)
            } catch (e: UninitializedPropertyAccessException) {
                Util.showLog("", e.localizedMessage)
            }
        }
    }
}