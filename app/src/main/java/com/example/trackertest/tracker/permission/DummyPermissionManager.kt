package com.example.trackertest.tracker.permission

class DummyPermissionManager : PermissionManagerInterface {
    override fun checkPermissions(){}

    override fun request(
        permissions: Array<String>,
        onResult: ((result: Boolean) -> Unit)?
    ){}
}