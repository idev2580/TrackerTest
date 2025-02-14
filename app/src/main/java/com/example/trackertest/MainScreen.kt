package com.example.trackertest

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.navigation.compose.NavHost
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.trackertest.tracker.collector.samsunghealth.ActiveCaloriesBurnedGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.ActiveTimeGoalCollector
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
import com.example.trackertest.ui.NonsessionDataPanel
import com.example.trackertest.ui.SessionDataPanel
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes

enum class MainScreen(){
    MAIN_SELECTION_SCREEN,
    NONSESSION_DATA,
    BLOOD_OXYGEN,
    HEART_RATE,
    SKIN_TEMPERATURE,
}

suspend fun checkAndRequestPermissions(context: Context, activity:MainActivity, permSet:Set<Permission>){
    val store: HealthDataStore = HealthDataService.getStore(context)
    val isAllAllowed:Boolean = store.getGrantedPermissions(permSet).containsAll(permSet)

    if(!isAllAllowed){
        store.requestPermissions(permSet, activity)
    }
}

@Composable
fun MainScreen(
    activity:MainActivity,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
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
    val tracker: SampleTracker by remember { mutableStateOf(SampleTracker(context)) }
    var isStarted by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        try {
            checkAndRequestPermissions(context, activity, permSet)
        } catch (e: Exception) {
            Log.d("TRACKER_TEST", "FAILED TO GET PERMISSION")
        }
    }


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = "TrackerTest",
            modifier = Modifier.padding(16.dp),
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Button(onClick = {
            if (isStarted) {
                tracker.stop()
            } else {
                tracker.start()
            }
            isStarted = !isStarted
        }) {
            Text(text = if (isStarted) "Stop" else "Start")
        }

        NavHost(
            navController = navController,
            startDestination = MainScreen.MAIN_SELECTION_SCREEN.name,
            modifier = Modifier.fillMaxSize()
        ){
            composable(route = MainScreen.MAIN_SELECTION_SCREEN.name){
                LazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    item {
                        Button(
                            onClick = {
                                navController.navigate(MainScreen.NONSESSION_DATA.name)
                            },
                            modifier=Modifier.fillMaxWidth()
                        ) {
                            Text(text="Others")
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                navController.navigate(MainScreen.BLOOD_OXYGEN.name)
                            },
                            modifier=Modifier.fillMaxWidth()
                        ) {
                            Text(text="BloodOxygen")
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                navController.navigate(MainScreen.HEART_RATE.name)
                            },
                            modifier=Modifier.fillMaxWidth()
                        ) {
                            Text(text="HeartRate")
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                navController.navigate(MainScreen.SKIN_TEMPERATURE.name)
                            },
                            modifier=Modifier.fillMaxWidth()
                        ) {
                            Text(text="SkinTemperature")
                        }
                    }
                }
            }
            composable(route = MainScreen.NONSESSION_DATA.name){
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        NonsessionDataPanel(
                            "ActiveCaloriesBurnedGoal", tracker.acbCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as ActiveCaloriesBurnedGoalCollector.Entity
                            val valueMap = mapOf(
                                Pair("goalSetTime", item.goalSetTime.toString()),
                                Pair("activeCaloriesBurned", item.activeCaloriesBurned.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "ActiveTimeGoal", tracker.atCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as ActiveTimeGoalCollector.Entity
                            val valueMap = mapOf(
                                Pair("goalSetTime", item.goalSetTime.toString()),
                                Pair("activeTime", item.activeTime.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "BloodPressure", tracker.bpCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as BloodPressureCollector.Entity
                            val valueMap = mapOf(
                                Pair("uid", item.uid),
                                Pair("timestamp", item.timestamp.toString()),
                                Pair("systolic", item.systolic.toString()),
                                Pair("diastolic", item.diastolic.toString()),
                                Pair("pulseRate", item.pulseRate.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "BodyComposition", tracker.bodyCompositionCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as BodyCompositionCollector.Entity
                            val valueMap = mapOf(
                                Pair("uid", item.uid),
                                Pair("timestamp", item.timestamp.toString()),
                                Pair("bodyFatRatio", item.bodyFatRatio.toString()),
                                Pair("weight", item.weight.toString()),
                                Pair("height", item.height.toString()),
                                Pair("muscleMass", item.muscleMass.toString()),
                                Pair("skeletalMuscleRatio", item.skeletalMuscleRatio.toString()),
                                Pair("totalBodyWater", item.totalBodyWater.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "Device", tracker.devCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as DeviceCollector.Entity
                            val valueMap = mapOf(
                                Pair("id", item.id),
                                Pair("deviceType", item.deviceType),
                                Pair("model", item.model),
                                Pair("name", item.name),
                                Pair("manufacturer", item.manufacturer)
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "NutritionGoal", tracker.nutCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as NutritionGoalCollector.Entity
                            val valueMap = mapOf(
                                Pair("goalSetTime", item.goalSetTime.toString()),
                                Pair("calories", item.calories.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "SleepGoal", tracker.sleepGoalCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as SleepGoalCollector.Entity
                            val valueMap = mapOf(
                                Pair("goalSetTime", item.goalSetTime.toString()),
                                Pair("wakeUpTime", item.wakeUpTime.toString()),
                                Pair("bedTime", item.bedTime.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "Step", tracker.stepCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as StepCollector.Entity
                            val valueMap = mapOf(
                                Pair("startTime", item.startTime.toString()),
                                Pair("endTime", item.endTime.toString()),
                                Pair("steps", item.steps.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "StepGoal", tracker.stepGoalCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as StepGoalCollector.Entity
                            val valueMap = mapOf(
                                Pair("goalSetTime", item.goalSetTime.toString()),
                                Pair("steps", item.steps.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "WaterIntake", tracker.waterCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as WaterIntakeCollector.Entity
                            val valueMap = mapOf(
                                Pair("uid", item.uid),
                                Pair("timestamp", item.timestamp.toString()),
                                Pair("amount", item.amount.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "WaterIntakeGoal", tracker.waterGoalCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as WaterIntakeGoalCollector.Entity
                            val valueMap = mapOf(
                                Pair("goalSetTime", item.goalSetTime.toString()),
                                Pair("amount", item.amount.toString())
                            )
                            valueMap
                        }
                    }
                }
            }
            composable(route = MainScreen.BLOOD_OXYGEN.name){
                SessionDataPanel(
                    "BloodOxygen",
                    tracker.boCollector.metadataStorage,
                    tracker.boCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxSize(),
                    { it ->
                        val item = it as BloodOxygenCollector.MetadataEntity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("oxygenSaturation", item.oxygenSaturation.toString())
                        )
                        valueMap
                    },
                    { it ->
                        val item = it as BloodOxygenCollector.Entity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("oxygenSaturation", item.oxygenSaturation.toString()),
                            Pair("max", item.max.toString()),
                            Pair("min", item.min.toString())
                        )
                        valueMap
                    }
                )
            }
            composable(route = MainScreen.HEART_RATE.name){
                SessionDataPanel(
                    "HeartRate",
                    tracker.hrCollector.metadataStorage,
                    tracker.hrCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxSize(),
                    { it ->
                        val item = it as HeartRateCollector.MetadataEntity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("heartRateSaturation", item.heartRate.toString())
                        )
                        valueMap
                    },
                    { it ->
                        val item = it as HeartRateCollector.Entity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("heartRateSaturation", item.heartRate.toString()),
                            Pair("max", item.max.toString()),
                            Pair("min", item.min.toString())
                        )
                        valueMap
                    }
                )
            }
            composable(route = MainScreen.SKIN_TEMPERATURE.name){
                SessionDataPanel(
                    "SkinTemperature",
                    tracker.stCollector.metadataStorage,
                    tracker.stCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxSize(),
                    { it ->
                        val item = it as SkinTemperatureCollector.MetadataEntity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("skinTemperature", item.skinTemperature.toString())
                        )
                        valueMap
                    },
                    { it ->
                        val item = it as SkinTemperatureCollector.Entity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("skinTemperature", item.skinTemperature.toString()),
                            Pair("max", item.max.toString()),
                            Pair("min", item.min.toString())
                        )
                        valueMap
                    }
                )
            }
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()){

        }

        /*LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                NonsessionDataPanel(
                    "ActiveCaloriesBurnedGoal", tracker.acbCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as ActiveCaloriesBurnedGoalCollector.Entity
                    val valueMap = mapOf(
                        Pair("goalSetTime", item.goalSetTime.toString()),
                        Pair("activeCaloriesBurned", item.activeCaloriesBurned.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "ActiveTimeGoal", tracker.atCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as ActiveTimeGoalCollector.Entity
                    val valueMap = mapOf(
                        Pair("goalSetTime", item.goalSetTime.toString()),
                        Pair("activeTime", item.activeTime.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "BloodPressure", tracker.bpCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as BloodPressureCollector.Entity
                    val valueMap = mapOf(
                        Pair("uid", item.uid),
                        Pair("timestamp", item.timestamp.toString()),
                        Pair("systolic", item.systolic.toString()),
                        Pair("diastolic", item.diastolic.toString()),
                        Pair("pulseRate", item.pulseRate.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "BodyComposition", tracker.bodyCompositionCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as BodyCompositionCollector.Entity
                    val valueMap = mapOf(
                        Pair("uid", item.uid),
                        Pair("timestamp", item.timestamp.toString()),
                        Pair("bodyFatRatio", item.bodyFatRatio.toString()),
                        Pair("weight", item.weight.toString()),
                        Pair("height", item.height.toString()),
                        Pair("muscleMass", item.muscleMass.toString()),
                        Pair("skeletalMuscleRatio", item.skeletalMuscleRatio.toString()),
                        Pair("totalBodyWater", item.totalBodyWater.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "Device", tracker.devCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as DeviceCollector.Entity
                    val valueMap = mapOf(
                        Pair("id", item.id),
                        Pair("deviceType", item.deviceType),
                        Pair("model", item.model),
                        Pair("name", item.name),
                        Pair("manufacturer", item.manufacturer)
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "NutritionGoal", tracker.nutCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as NutritionGoalCollector.Entity
                    val valueMap = mapOf(
                        Pair("goalSetTime", item.goalSetTime.toString()),
                        Pair("calories", item.calories.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "SleepGoal", tracker.sleepGoalCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as SleepGoalCollector.Entity
                    val valueMap = mapOf(
                        Pair("goalSetTime", item.goalSetTime.toString()),
                        Pair("wakeUpTime", item.wakeUpTime.toString()),
                        Pair("bedTime", item.bedTime.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "Step", tracker.stepCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as StepCollector.Entity
                    val valueMap = mapOf(
                        Pair("startTime", item.startTime.toString()),
                        Pair("endTime", item.endTime.toString()),
                        Pair("steps", item.steps.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "StepGoal", tracker.stepGoalCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as StepGoalCollector.Entity
                    val valueMap = mapOf(
                        Pair("goalSetTime", item.goalSetTime.toString()),
                        Pair("steps", item.steps.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "WaterIntake", tracker.waterCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as WaterIntakeCollector.Entity
                    val valueMap = mapOf(
                        Pair("uid", item.uid),
                        Pair("timestamp", item.timestamp.toString()),
                        Pair("amount", item.amount.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "WaterIntakeGoal", tracker.waterGoalCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as WaterIntakeGoalCollector.Entity
                    val valueMap = mapOf(
                        Pair("goalSetTime", item.goalSetTime.toString()),
                        Pair("amount", item.amount.toString())
                    )
                    valueMap
                }
            }
            item {
                SessionDataPanel(
                    "BloodOxygen",
                    tracker.boCollector.metadataStorage,
                    tracker.boCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth(),
                    { it ->
                        val item = it as BloodOxygenCollector.MetadataEntity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("oxygenSaturation", item.oxygenSaturation.toString())
                        )
                        valueMap
                    },
                    { it ->
                        val item = it as BloodOxygenCollector.Entity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("oxygenSaturation", item.oxygenSaturation.toString()),
                            Pair("max", item.max.toString()),
                            Pair("min", item.min.toString())
                        )
                        valueMap
                    }
                )
            }
            item {
                SessionDataPanel(
                    "HeartRate",
                    tracker.hrCollector.metadataStorage,
                    tracker.hrCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth(),
                    { it ->
                        val item = it as HeartRateCollector.MetadataEntity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("heartRateSaturation", item.heartRate.toString())
                        )
                        valueMap
                    },
                    { it ->
                        val item = it as HeartRateCollector.Entity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("heartRateSaturation", item.heartRate.toString()),
                            Pair("max", item.max.toString()),
                            Pair("min", item.min.toString())
                        )
                        valueMap
                    }
                )
            }
            item {
                SessionDataPanel(
                    "SkinTemperature",
                    tracker.stCollector.metadataStorage,
                    tracker.stCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth(),
                    { it ->
                        val item = it as SkinTemperatureCollector.MetadataEntity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("skinTemperature", item.skinTemperature.toString())
                        )
                        valueMap
                    },
                    { it ->
                        val item = it as SkinTemperatureCollector.Entity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("skinTemperature", item.skinTemperature.toString()),
                            Pair("max", item.max.toString()),
                            Pair("min", item.min.toString())
                        )
                        valueMap
                    }
                )
            }
        }*/
    }
}