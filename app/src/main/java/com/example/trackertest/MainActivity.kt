package com.example.trackertest

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.trackertest.tracker.collector.core.AbstractCollector
import com.example.trackertest.tracker.collector.core.CollectorState
import com.example.trackertest.tracker.collector.samsunghealth.ActiveCaloriesBurnedGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.ActiveTimeGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.NutritionGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.SleepGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.StepCollector
import com.example.trackertest.tracker.collector.samsunghealth.StepGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.StepGoalCollector.Config
import com.example.trackertest.tracker.data.DummySingletonStorage
import com.example.trackertest.tracker.permission.DummyPermissionManager
import com.example.trackertest.ui.theme.TrackerTestTheme
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrackerTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        activity=this,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

suspend fun checkAndRequestPermissions(context: Context, activity:MainActivity, permSet:Set<Permission>){
    val store: HealthDataStore = HealthDataService.getStore(context)
    val isAllAllowed:Boolean = store.getGrantedPermissions(permSet).containsAll(permSet)

    if(!isAllAllowed){
        store.requestPermissions(permSet, activity)
    }
}

@Composable
fun Greeting(activity:MainActivity, modifier: Modifier = Modifier) {
    val context = LocalContext.current
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
    val acbCollector by remember {mutableStateOf(
        ActiveCaloriesBurnedGoalCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<ActiveCaloriesBurnedGoalCollector.Config>(ActiveCaloriesBurnedGoalCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )}
    val atCollector by remember {mutableStateOf(
        ActiveTimeGoalCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<ActiveTimeGoalCollector.Config>(ActiveTimeGoalCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )}
    val nutCollector by remember {mutableStateOf(
        NutritionGoalCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<NutritionGoalCollector.Config>(NutritionGoalCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )}
    val stepGoalCollector by remember {mutableStateOf(
        StepGoalCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<StepGoalCollector.Config>(StepGoalCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )}
    val sleepGoalCollector by remember {mutableStateOf(
        SleepGoalCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<SleepGoalCollector.Config>(SleepGoalCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )}
    val stepCollector by remember {mutableStateOf(
        StepCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<StepCollector.Config>(StepCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )}

    LaunchedEffect(context) {
        try {
            checkAndRequestPermissions(context, activity, permSet)
        } catch (e:Exception){
            Log.d("TRACKER_TEST","FAILED TO GET PERMISSION")
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier=Modifier.fillMaxSize()
    ){
        Text(
            text = "TrackerTest",
            modifier = modifier
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ){
            Button(onClick={
                acbCollector.start()
                atCollector.start()
                nutCollector.start()
                stepGoalCollector.start()
                sleepGoalCollector.start()
                stepCollector.start()
            }){
                Text(text = "Start")
            }
            Button(onClick={
                acbCollector.stop()
                atCollector.stop()
                nutCollector.stop()
                stepGoalCollector.stop()
                sleepGoalCollector.stop()
                stepCollector.stop()
            }){
                Text(text = "Stop")
            }
        }
    }
}