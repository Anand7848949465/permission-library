package com.iserveu.permission.bluetooth

interface ProceedOperationListener {
    fun proceedWithUi()
    fun onDeniedToGrantPermission()
    fun onBluetoothNotSupported()
}