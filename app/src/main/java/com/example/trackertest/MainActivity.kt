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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    MainScreen(
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
fun MainScreen(activity:MainActivity, modifier: Modifier = Modifier) {
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
    val tracker:SampleTracker by remember{mutableStateOf(SampleTracker(context))}
    var isStarted by remember {mutableStateOf(false)}

    LaunchedEffect(context) {
        try {
            checkAndRequestPermissions(context, activity, permSet)
        } catch (e:Exception){
            Log.d("TRACKER_TEST","FAILED TO GET PERMISSION")
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier=modifier.fillMaxSize()
    ){
        Text(
            text = "TrackerTest",
            modifier = Modifier.padding(16.dp),
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Button(onClick={
            if(isStarted){
                tracker.stop()
            } else {
                tracker.start()
            }
            isStarted = !isStarted
        }){
            Text(text = if (isStarted) "Stop" else  "Start")
        }

    }
}
@Composable
fun NamedPanel(name:String, modifier:Modifier = Modifier, content:@Composable ()->Unit){
    Column(
        modifier=modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(text=name, fontWeight=FontWeight.SemiBold)
        content()
    }
}

@Composable
fun NonsessionDataPanel(name:String, modifier: Modifier = Modifier, content:@Composable ()->Unit){

}