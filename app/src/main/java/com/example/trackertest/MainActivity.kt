package com.example.trackertest

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackertest.tracker.collector.core.AbstractCollector
import com.example.trackertest.tracker.collector.core.CollectorState
import com.example.trackertest.tracker.collector.core.DataEntity
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
import com.example.trackertest.ui.NonsessionDataPanel
import com.example.trackertest.ui.SessionDataPanel
import com.example.trackertest.ui.theme.Purple40
import com.example.trackertest.ui.theme.Purple80
import com.example.trackertest.ui.theme.PurpleGrey80
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
