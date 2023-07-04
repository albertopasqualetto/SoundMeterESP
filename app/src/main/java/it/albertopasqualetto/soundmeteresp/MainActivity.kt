package it.albertopasqualetto.soundmeteresp

// min = 0 dB, max = 120 dB for visualization purposes


import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import it.albertopasqualetto.soundmeteresp.ui.theme.SoundMeterESPTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    companion object {
        val TAG = MainActivity::class.simpleName

        private val PROGRESS_BAR_HEIGHT = 50.dp
        private val PROGRESS_BAR_WIDTH = 200.dp

        private var isRunning = false   // used instead of MeterService.isRecording to prevent race conditions

        var coldStart = true

        fun dBToProgress(dB : Float) : Float {
            return dB/120 // scale from [0dB-120dB] to [0-1]
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // keep screen on (besides the wakelock)

        Log.d(TAG, "onCreate!")

        // Register a callback that calls the finish() method when the back button is pressed.
        this.onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val i = Intent(applicationContext, MeterService::class.java)
                stopService(i)
                coldStart = true
                finish()
            }
        })

        Log.d(TAG, "onCreate: coldStart = $coldStart")

        setContent {
            SoundMeterESPTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
                    if (coldStart){
                        Values.resetAll()
                        if(permissionState.status.isGranted) {
                            val i = Intent(applicationContext, MeterService::class.java)
                            startForegroundService(i)
                            isRunning = true
                            Log.d(TAG, "onCreate: start service")
                        }
                    }
                    AppContent(permissionState)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause!")
        val i = Intent(applicationContext, MeterService::class.java)
        val wasRecording = isRunning
        stopService(i)
        if (wasRecording && !isFinishing) {
            i.putExtra(MeterService.MAIN_ACTIVITY_PAUSE, true)
            startForegroundService(i)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume!")
        if (isRunning) {
            Log.d(TAG, "onResume: restart service (isRunning)")
            val i = Intent(applicationContext, MeterService::class.java)
            startForegroundService(i)
        }

        // redraw chart
        Charts.ONE_SEC_LEFT.redraw()
        Charts.ONE_SEC_RIGHT.redraw()
        Charts.FIVE_MIN_LEFT.redraw()
        Charts.FIVE_MIN_RIGHT.redraw()
    }


    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
        ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalPermissionsApi::class
    )
    @Preview(name = "Vertical AppContent", showBackground = true)   // if horizontal preview is wanted, then also OneSecView and FiveMinView previews must be horizontal (or rewrite code to handle this case)
    @Composable
    fun AppContent(permissionState: PermissionState = FakePermissionState(PermissionStatus.Granted)) {
        val windowSizeClass = if(!LocalInspectionMode.current) calculateWindowSizeClass(this) else WindowSizeClass.calculateFromSize(DpSize(360.dp, 760.dp))   // fallback WindowSizeClass used for preview

        val tabs = listOf("Last second", "5 minutes History")
        val pagerState = rememberPagerState(initialPage = 0)
        val coroutineScope = rememberCoroutineScope()
        var playOrPauseState by remember { mutableStateOf(if(isRunning) 1 else 0) }
        val showPermissionDialog = remember { mutableStateOf(false) }
        Column(modifier = Modifier.fillMaxWidth()) {
            CenterAlignedTopAppBar(title = { Text("Sound Meter", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    actions = { IconButton(onClick = {
                                        Log.d(TAG, "click, playOrPauseState: $playOrPauseState, showPermissionDialog: $showPermissionDialog")
                                        if (!permissionState.status.isGranted){
                                            showPermissionDialog.value = true
                                            permissionState.launchPermissionRequest()
                                        } else {
                                            if (playOrPauseState == 0){ // paused
                                                val i = Intent(applicationContext, MeterService::class.java)
                                                startForegroundService(i)
                                                isRunning = true
                                            } else { // playing
                                                val i = Intent(applicationContext, MeterService::class.java)
                                                stopService(i)
                                                isRunning = false
                                            }
                                            playOrPauseState = if (playOrPauseState==0) 1 else 0
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (playOrPauseState==0) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                            contentDescription = "Start recording"
                                        )
                                        }
                                    }
            )

            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(text = { Text(title) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = if (windowSizeClass.heightSizeClass != WindowHeightSizeClass.Compact){
                            { when (index) {
                                0 -> Icon(Icons.Default.Hearing, contentDescription = "Last second")
                                1 ->Icon(Icons.Default.History, contentDescription = "Last 5 minutes")
                                else -> Icon(Icons.Default.Star, contentDescription = "")   // unused
                        }}} else null
                    )
                }
            }
            HorizontalPager(
                pageCount = tabs.size,
                state = pagerState,
                beyondBoundsPageCount = 2
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> OneSecView()
                    1 -> FiveMinView()
                }
            }

            if (!permissionState.status.isGranted){
                Log.d(TAG, "AppContent: permission NOT granted")
                playOrPauseState = 0
                isRunning = false
                val i = Intent(applicationContext, MeterService::class.java)
                stopService(i)
                showPermissionDialog.value = true
                if (permissionState.status.shouldShowRationale) {
                    Log.d(TAG, "AppContent: shouldShowRationale")
                    NoPermissionDialog(openDialog = showPermissionDialog, shouldShowRationale = true)
                } else {
                    Log.d(TAG, "AppContent: NO shouldShowRationale")
                    NoPermissionDialog(openDialog = showPermissionDialog, shouldShowRationale = false)
                }
            } else {
                if (coldStart)
                    playOrPauseState = 1
                Log.d(TAG, "AppContent: permission granted")
            }
            coldStart = false
        }

        SideEffect {
            Log.d(TAG, "AppContent: recomposing")
            if (!permissionState.status.isGranted){
                permissionState.launchPermissionRequest()
            }
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Preview(name = "Vertical OneSecView", showBackground = true)
    @Composable
    fun OneSecView() {
        var leftdb by rememberSaveable { mutableStateOf("Waiting left...") }
        var rightdb by rememberSaveable { mutableStateOf("Waiting right...") }

        var progressLeft by rememberSaveable { mutableStateOf(0.0f) }
        val animatedProgressLeft by animateFloatAsState(
            targetValue = progressLeft,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
        )
        var progressRight by rememberSaveable { mutableStateOf(0.0f) }
        val animatedProgressRight by animateFloatAsState(
            targetValue = progressRight,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
        )

        var updateChartOneLeft by remember { mutableStateOf(0f) }
        var updateChartOneRight by remember { mutableStateOf(0f) }

        val windowSizeClass = if(!LocalInspectionMode.current) calculateWindowSizeClass(this) else WindowSizeClass.calculateFromSize(DpSize(360.dp, 760.dp))   // fallback WindowSizeClass used for preview
        if (windowSizeClass.heightSizeClass != WindowHeightSizeClass.Compact) {
            Log.d(TAG, "In Column arrangement")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {  // outline
                LeftOneSecView(leftdb, animatedProgressLeft, updateChartOneLeft, modifier= Modifier.weight(1f))
                Spacer(modifier=Modifier.weight(0.1f))
                RightOneSecView(rightdb, animatedProgressRight, updateChartOneRight, modifier= Modifier.weight(1f))
            }
        } else {
            Log.d(TAG, "In Row arrangement")
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {  // outline
                LeftOneSecView(leftdb, animatedProgressLeft, updateChartOneLeft, modifier= Modifier.weight(1f))
                Spacer(modifier=Modifier.weight(0.1f))
                RightOneSecView(rightdb, animatedProgressRight, updateChartOneRight, modifier= Modifier.weight(1f))
            }
        }


        LaunchedEffect(key1 = Unit) {
            var countToSec = 0  // count to 16,6 = 1 sec
            while (true) {
                if (isRunning) {
                    countToSec++
                    if (countToSec >= 62) { // 1000/16 ~= 62
                        countToSec = 0
                        val dBLeftMax = Values.lastSecDbLeftList.lastOrNull() ?: continue
                        val dBRightMax = Values.lastSecDbRightList.lastOrNull() ?: continue
                        Log.d(TAG, "leftMax: $dBLeftMax, rightMax: $dBRightMax")
                        leftdb = "%.${2}f".format(dBLeftMax)
                        progressLeft = dBToProgress(dBLeftMax)
                        rightdb = dBRightMax.toString()
                        rightdb = "%.${2}f".format(dBRightMax)
                        progressRight = dBToProgress(dBRightMax)
                    }

                    Log.d(TAG, "lastSecDbLeftList size: ${Values.lastSecDbLeftList.size}, lastSecDbRightList size: ${Values.lastSecDbRightList.size}")
                    Log.d(TAG, "lastLeft: ${Values.lastLeft}, lastRight: ${Values.lastRight}")
                    if (Values.lastLeft != 0f) updateChartOneLeft = Values.lastLeft
                    if (Values.lastRight != 0f) updateChartOneRight = Values.lastRight
                }

                delay(16)  // ~16 ms = 60 Hz (refresh rate of the screen)
            }
        }
    }

    @Composable
    fun LeftOneSecView(leftdb: String, progressLeft: Float, updateChartOneLeft: Float, modifier: Modifier = Modifier) {
        Row(modifier = modifier.padding(2.dp)) { // left
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {  // arrangement in column
                Text(text = "Left channel", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = modifier.fillMaxWidth()) {
                    Text(text = "$leftdb dB", modifier= Modifier
                        .padding(9.dp, 0.dp, 0.dp, 0.dp)
                        .weight(1f))
                    LinearProgressIndicator(
                        modifier = modifier
                            .semantics(mergeDescendants = true) {}
                            .requiredHeight(PROGRESS_BAR_HEIGHT)
                            .requiredWidth(PROGRESS_BAR_WIDTH)
                            .weight(2f)
                            .padding(0.dp, 0.dp, 17.dp, 0.dp),
                        progress = progressLeft,
                    )
                }

                Charts.ONE_SEC_LEFT(updateTrigger = updateChartOneLeft, modifier = modifier.fillMaxSize())
            }
        }
    }

    @Composable
    fun RightOneSecView(rightdb: String, progressRight: Float, updateChartOneRight: Float, modifier: Modifier = Modifier){
        Row(modifier = modifier.padding(2.dp)) { // right
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {  // arrangement in column
                Text(text = "Right channel", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround, modifier = modifier.fillMaxWidth()) {
                    Text(text = "$rightdb dB", modifier= Modifier
                        .padding(9.dp, 0.dp, 0.dp, 0.dp)
                        .weight(1f))
                    LinearProgressIndicator(
                        modifier = modifier
                            .semantics(mergeDescendants = true) {}
                            .requiredHeight(PROGRESS_BAR_HEIGHT)
                            .requiredWidth(PROGRESS_BAR_WIDTH)
                            .weight(1.5f)
                            .padding(0.dp, 0.dp, 17.dp, 0.dp),
                        progress = progressRight,
                    )
                }

                Charts.ONE_SEC_RIGHT(updateTrigger = updateChartOneRight, modifier = modifier.fillMaxSize())
            }
        }
    }


    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Preview(name = "Vertical FiveMinView", showBackground = true)
    @Composable
    fun FiveMinView(){
        var onUpdateChartFiveLeft by remember { mutableStateOf(0f) }
        var onUpdateChartFiveRight by remember { mutableStateOf(0f) }


        val windowSizeClass = if(!LocalInspectionMode.current) calculateWindowSizeClass(this) else WindowSizeClass.calculateFromSize(DpSize(360.dp, 760.dp))   // fallback WindowSizeClass used for preview
        if (windowSizeClass.heightSizeClass != WindowHeightSizeClass.Compact) {
            Log.d(TAG, "In Column arrangement")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {  // outline
                LeftFiveMinView(onUpdateChartFiveLeft, modifier= Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(0.05f))
                RightFiveMinView(onUpdateChartFiveRight, modifier= Modifier.weight(1f))
            }
        } else {
            Log.d(TAG, "In Row arrangement")
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {  // outline
                LeftFiveMinView(onUpdateChartFiveLeft, modifier= Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(0.05f))
                RightFiveMinView(onUpdateChartFiveRight, modifier= Modifier.weight(1f))
            }
        }


        LaunchedEffect(key1 = Unit, block = {
            while (true){
                if(isRunning){
                    Log.d(TAG, "Launched effect: FiveMinView")

                    // used to trigger recomposition:
                    onUpdateChartFiveLeft = (0..1_000_000).random().toFloat()
                    onUpdateChartFiveRight = (0..1_000_000).random().toFloat()
                }


                delay(1000)
            }
        })

    }


    @Composable
    fun LeftFiveMinView(updateChartFiveLeft: Float, modifier: Modifier = Modifier){
        Row(modifier = modifier.padding(2.dp)) { // left
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {  // arrangement in column
                Text(text = "Left channel", fontWeight = FontWeight.Bold)
                Charts.FIVE_MIN_LEFT(updateTrigger = updateChartFiveLeft, modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp))
            }
        }
    }

    @Composable
    fun RightFiveMinView(updateChartFiveRight: Float, modifier: Modifier = Modifier){
        Row(modifier = modifier.padding(2.dp)) { // right
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {  // arrangement in column
                Text(text = "Right channel", fontWeight = FontWeight.Bold)
                Charts.FIVE_MIN_RIGHT(updateTrigger = updateChartFiveRight, modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp))
            }
        }
    }


    @Preview
    @Composable
    fun NoPermissionDialog(openDialog: MutableState<Boolean> = mutableStateOf(true), shouldShowRationale: Boolean = true) {
        Log.d(TAG, "NoPermissionDialog, onShowPermissionDialog: $openDialog")

        if (!openDialog.value) return
        AlertDialog(
            onDismissRequest = { openDialog.value = false },
            icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
            title = { Text(text = "Permissions not granted") },
            text = { Text(text = "Please grant permission to record audio in order to use this app") },
            dismissButton = if(!shouldShowRationale) {{
                    Button(onClick = { openDialog.value = false }) {
                        Text(text = "Later")
                    }
                }} else null,
            confirmButton = if(!shouldShowRationale) {
                {
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        intent.putExtra(":settings:show_fragment_args", bundleOf(":settings:fragment_args_key" to "permission_settings"))   // highlight the permission row
                        startActivity(intent)
                    }) {
                        Text(text = "Go to settings")
                    }
                }} else {
                {
                    Button(onClick = { openDialog.value = false }) {
                        Text(text = "OK")
                    }

                }}
        )
    }

    @ExperimentalPermissionsApi
    private class FakePermissionState(  // fake permission state to be used in preview
        override val status: PermissionStatus,
        override val permission: String = "Not used, this is fake!"
    ): PermissionState {
        override fun launchPermissionRequest(): Unit = throw NotImplementedError()
    }

}



