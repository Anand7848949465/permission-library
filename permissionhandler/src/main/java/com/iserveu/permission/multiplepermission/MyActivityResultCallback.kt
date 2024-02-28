package com.iserveu.permission.multiplepermission

import androidx.activity.result.ActivityResult
import com.iserveu.permission.utils.Util

class MyActivityResultCallback {
    private lateinit var multiplePermissionCallback: MultiplePermissionCallback

    fun setCallback(multiplePermissionCallBack: MultiplePermissionCallback) {
        this.multiplePermissionCallback = multiplePermissionCallBack
    }

    fun onActivityResult(permissions: Any) {
        if (permissions is Map<*, *>) {
            multiplePermissionCallback.handleMultiplePermissionCallBack(permissions as Map<String, Boolean>)
        } else if (permissions is ActivityResult) {
            try {
                multiplePermissionCallback.handleActivityResultCallBack(permissions)
            } catch (e: UninitializedPropertyAccessException) {
                Util.showLog("", e.localizedMessage)
            }
        }
    }
}