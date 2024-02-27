package com.iserveu.permission.multiplepermission

import androidx.activity.result.ActivityResult

interface MultiplePermissionCallback {
    fun  handleMultiplePermissionCallBack(permissions: Map<String, Boolean>)
    fun  handleActivityResultCallBack(activityResult: ActivityResult)
}