package com.example.trackertest.tracker.permission

import kotlinx.coroutines.flow.StateFlow

interface PermissionManagerInterface {
    /* Initialize the permission manager with the activity */
    //fun initialize(activity: PermissionActivity)

    //val permissionStateFlow: StateFlow<Map<String, PermissionState>>

    /*To update current status: Used for Special Permission Update */
    fun checkPermissions()

    fun request(
        permissions: Array<String>,
        onResult: ((result: Boolean) -> Unit)? = null
    )
}