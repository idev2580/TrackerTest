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
import com.example.trackertest.tracker.collector.samsunghealth.AbstractMeasurementSessionCollector
import com.example.trackertest.tracker.collector.samsunghealth.ActiveCaloriesBurnedGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.ActiveTimeGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.ActivitySummaryCollector
import com.example.trackertest.tracker.collector.samsunghealth.BloodOxygenCollector
import com.example.trackertest.tracker.collector.samsunghealth.BloodPressureCollector
import com.example.trackertest.tracker.collector.samsunghealth.BodyCompositionCollector
import com.example.trackertest.tracker.collector.samsunghealth.DeviceCollector
import com.example.trackertest.tracker.collector.samsunghealth.HeartRateCollector
import com.example.trackertest.tracker.collector.samsunghealth.NutritionGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.SkinTemperatureCollector
import com.example.trackertest.tracker.collector.samsunghealth.SleepGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.StepCollector
import com.example.trackertest.tracker.collector.samsunghealth.StepGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.WaterIntakeCollector
import com.example.trackertest.tracker.collector.samsunghealth.WaterIntakeGoalCollector
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
    val asCollector by remember{mutableStateOf(
        ActivitySummaryCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<ActivitySummaryCollector.Config>(ActivitySummaryCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )}
    val boCollector by remember{mutableStateOf(
        BloodOxygenCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<BloodOxygenCollector.Config>(BloodOxygenCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )}
    val bpCollector by remember{ mutableStateOf(
        BloodPressureCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<BloodPressureCollector.Config>(BloodPressureCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )}
    val bodyCompositionCollector by remember{ mutableStateOf(
        BodyCompositionCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<BodyCompositionCollector.Config>(BodyCompositionCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )}
    val devCollector by remember {mutableStateOf(
        DeviceCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<DeviceCollector.Config>(DeviceCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )}
    val hrCollector by remember {mutableStateOf(
        HeartRateCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<HeartRateCollector.Config>(HeartRateCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )}
    val stCollector by remember {mutableStateOf(
        SkinTemperatureCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<SkinTemperatureCollector.Config>(SkinTemperatureCollector.defaultConfig),
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
    val waterCollector by remember { mutableStateOf(
        WaterIntakeCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<WaterIntakeCollector.Config>(WaterIntakeCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )}
    val waterGoalCollector by remember { mutableStateOf(
        WaterIntakeGoalCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<WaterIntakeGoalCollector.Config>(WaterIntakeGoalCollector.defaultConfig),
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
                asCollector.start()
                boCollector.start()
                bpCollector.start()
                bodyCompositionCollector.start()
                devCollector.start()
                hrCollector.start()
                stCollector.start()
                nutCollector.start()
                stepGoalCollector.start()
                sleepGoalCollector.start()
                stepCollector.start()
                waterCollector.start()
                waterGoalCollector.start()
            }){
                Text(text = "Start")
            }
            Button(onClick={
                acbCollector.stop()
                atCollector.stop()
                asCollector.stop()
                boCollector.stop()
                bpCollector.stop()
                bodyCompositionCollector.stop()
                devCollector.stop()
                hrCollector.stop()
                stCollector.stop()
                nutCollector.stop()
                stepGoalCollector.stop()
                sleepGoalCollector.stop()
                stepCollector.stop()
                waterCollector.stop()
                waterGoalCollector.stop()
            }){
                Text(text = "Stop")
            }
        }
    }
}