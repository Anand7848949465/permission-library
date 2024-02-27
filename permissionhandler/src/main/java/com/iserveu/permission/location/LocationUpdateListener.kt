package com.iserveu.permission.location

interface LocationUpdateListener {
    /**
     * Called when a new location update is available.
     *
     * @param latLong The latitude and longitude coordinates in the format "latitude,longitude".
     */
    fun onLocationUpdate(latLong: String?)
    fun proceedWithUi()
}