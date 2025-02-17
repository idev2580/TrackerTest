package com.example.trackertest

import android.content.Context
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.util.Consumer
import com.example.trackertest.tracker.permission.PermissionManagerInterface
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.AuthorizationException
import com.samsung.android.sdk.health.data.error.InvalidRequestException
import com.samsung.android.sdk.health.data.error.PlatformInternalException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes
import kotlinx.coroutines.Runnable

class SamsungHealthPermissionManager(val activity:MainActivity):PermissionManagerInterface {
    val permSet = setOf(
        Permission.of(DataTypes.ACTIVITY_SUMMARY, AccessType.READ),
        Permission.of(DataTypes.ACTIVE_CALORIES_BURNED_GOAL, AccessType.READ),
        Permission.of(DataTypes.ACTIVE_TIME_GOAL, AccessType.READ),
        Permission.of(DataTypes.BLOOD_GLUCOSE, AccessType.READ),
        Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.READ),
        Permission.of(DataTypes.BLOOD_PRESSURE, AccessType.READ),
        Permission.of(DataTypes.BODY_COMPOSITION, AccessType.READ),
        Permission.of(DataTypes.EXERCISE, AccessType.READ),
        Permission.of(DataTypes.EXERCISE_LOCATION, AccessType.READ),
        Permission.of(DataTypes.FLOORS_CLIMBED, AccessType.READ),
        Permission.of(DataTypes.HEART_RATE, AccessType.READ),
        Permission.of(DataTypes.NUTRITION, AccessType.READ),
        Permission.of(DataTypes.NUTRITION_GOAL, AccessType.READ),
        Permission.of(DataTypes.SKIN_TEMPERATURE, AccessType.READ),
        Permission.of(DataTypes.SLEEP, AccessType.READ),
        Permission.of(DataTypes.SLEEP_GOAL, AccessType.READ),
        Permission.of(DataTypes.STEPS, AccessType.READ),
        Permission.of(DataTypes.STEPS_GOAL, AccessType.READ),
        Permission.of(DataTypes.USER_PROFILE, AccessType.READ),
        Permission.of(DataTypes.WATER_INTAKE, AccessType.READ),
        Permission.of(DataTypes.WATER_INTAKE_GOAL, AccessType.READ)
    )

    fun checkAndRequestPermissions(context: Context,onResult: ((result: Boolean) -> Unit)?){
        val store: HealthDataStore = HealthDataService.getStore(context)
        store.getGrantedPermissionsAsync(permSet).setCallback(
            Looper.getMainLooper(),
            { res:Set<Permission> ->
                //Log.d("TAG", "getPermissionSuccess")
                if(!res.containsAll(permSet)){
                    store.requestPermissionsAsync(permSet, activity).setCallback(
                        Looper.getMainLooper(),
                        { res:Set<Permission> ->
                            val isGranted:Boolean = res.containsAll(permSet)
                            if(!isGranted){
                                //Still not have permissions
                                Toast.makeText(context, "모든 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
                            }
                            onResult?.invoke(isGranted)
                            //runnable.run()
                        },
                        { error:Throwable ->
                            if(error is ResolvablePlatformException && error.hasResolution){
                                error.resolve(activity)
                            }
                        }
                    )
                } else {
                    //runnable.run()
                }
            },
            { error:Throwable ->
                //Log.d("TAG", "getPermissionError")
                if (error is ResolvablePlatformException && error.hasResolution) {
                    error.resolve(activity)
                    // An exception to indicate that the Samsung Health platform is not ready to serve the specified operation.
                } else if (error is AuthorizationException) {
                    // An exception to indicate that the application is not authorized to perform given operation.
                    Toast.makeText(context, "삼성 헬스 개발자 모드가 활성화되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
                } else if (error is InvalidRequestException) {
                    // An exception to indicate that the application requests the given operation with invalid conditions.
                } else if (error is PlatformInternalException) {
                    // An exception to indicate that the Samsung Health platform experienced an internal error
                    // that cannot be resolved by the application.
                }
            }
        )
    }

    override fun checkPermissions(){
        //Not implemented
    }
    override fun request(
        permissions: Array<String>,
        onResult: ((result: Boolean) -> Unit)?
    ){
        checkAndRequestPermissions(activity, onResult)
    }
}