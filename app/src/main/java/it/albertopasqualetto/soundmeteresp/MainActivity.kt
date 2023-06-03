package it.albertopasqualetto.soundmeteresp

// min = 0 dB, max = 200 dB


// TODO rememberSaveable per salvare lo stato della rotazione dello schermo (e in altri casi?)
// TODO change 1000/60 to its result
// TODO fix layout margins

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import it.albertopasqualetto.soundmeteresp.ui.theme.SoundMeterESPTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    companion object {
        val TAG = MainActivity::class.simpleName

        private val PROGRESS_BAR_HEIGHT = 50.dp
        private val PROGRESS_BAR_WIDTH = 200.dp

        fun dBToProgress(dB : Float) : Float {
            return (dB/2)/100 // scale from 0dB-200dB to 0-1
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate!")

        // Register a callback that calls the finish() method when the back button is pressed.
        this.onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val i = Intent(applicationContext, MeterService::class.java)
                stopService(i)
                finish()
            }
        })

        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
//                Print("Permission granted")
            } else {
                // Explain to the user that the feature is unavailable because
                // the features requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
//                        return
        }

        val i = Intent(applicationContext, MeterService::class.java)
        i.putExtra(MeterService.REC_START, true)
        startForegroundService(i)


        setContent {
            SoundMeterESPTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        /*recorderThread = null
        if (meter?.state == AudioRecord.STATE_INITIALIZED && meter?.recordingState == AudioRecord.RECORDSTATE_RECORDING) meter?.stop() ?: Log.d(TAG, "onPause: meter is not recording")*/
    }

    override fun onResume() {
        super.onResume()
        // TODO redraw chart
        /*Charts.ONE_SEC_LEFT.redraw()
        Charts.ONE_SEC_RIGHT.redraw()
        Charts.FIVE_MIN_LEFT.redraw()
        Charts.FIVE_MIN_RIGHT.redraw()*/
        /*onUpdateChartFiveLeft = (0..1_000_000).random()
        onUpdateChartFiveRight = (0..1_000_000).random()*/
//        if (meter?.state == AudioRecord.STATE_INITIALIZED && meter?.recordingState == AudioRecord.RECORDSTATE_STOPPED) meter?.startRecording() ?: Log.d(TAG, "onResume: meter is not stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        /*if (meter?.state == AudioRecord.STATE_INITIALIZED && meter?.recordingState == AudioRecord.RECORDSTATE_STOPPED) meter?.stop() ?: Log.d(TAG, "onDestroy: meter is not stopped")
        if (meter?.state == AudioRecord.STATE_INITIALIZED) meter?.release() ?: Log.d(TAG, "onDestroy: meter is not initialized")
        meter = null*/
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
        ExperimentalMaterial3WindowSizeClassApi::class
    )
    @Preview(showBackground = true)
    @Composable
    fun AppContent(){
        val windowSizeClass = calculateWindowSizeClass(this)

        val tabs = listOf("Last second", "5 minutes History")
        val pagerState = rememberPagerState(initialPage = 0)
        val coroutineScope = rememberCoroutineScope()
        var playOrPauseState by remember { mutableStateOf(0) }

        Column(modifier = Modifier.fillMaxWidth()) {
            CenterAlignedTopAppBar(title = { Text("Sound Meter", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    actions = { IconButton(onClick = {
                                        if (playOrPauseState == 0){ // playing
                                            val i = Intent(applicationContext, MeterService::class.java)
                                            stopService(i)
                                        } else { // paused
                                            val i = Intent(applicationContext, MeterService::class.java)
                                            i.putExtra(MeterService.REC_START, true)
                                            startForegroundService(i)
                                        }

                                        playOrPauseState = if (playOrPauseState==0) 1 else 0
                                    }) {
                                        Icon(
                                            imageVector = if (playOrPauseState==0) Icons.Filled.Pause else Icons.Filled.PlayArrow,
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
                                else -> Icon(Icons.Default.Star, contentDescription = "")
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
        }
    }
    // FIXME ritardo

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    fun OneSecView() {
        var leftdb by remember { mutableStateOf("Waiting left...") }
        var rightdb by remember { mutableStateOf("Waiting right...") }

        var progressLeft by remember { mutableStateOf(0.0f) }
        val animatedProgressLeft by animateFloatAsState(
            targetValue = progressLeft,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
        )
        var progressRight by remember { mutableStateOf(0.0f) }
        val animatedProgressRight by animateFloatAsState(
            targetValue = progressRight,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
        )

        var updateChartOneLeft by remember { mutableStateOf(0f) }
        var updateChartOneRight by remember { mutableStateOf(0f) }

        val windowSizeClass = calculateWindowSizeClass(this)
        if (windowSizeClass.heightSizeClass != WindowHeightSizeClass.Compact) {
            Log.d(TAG, "In column arrangement")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp), verticalArrangement = Arrangement.SpaceEvenly
            ) {  // outline
                LeftOneSecView(leftdb, animatedProgressLeft, updateChartOneLeft, modifier= Modifier.weight(1f))
                Spacer(modifier=Modifier.weight(0.1f))
                RightOneSecView(rightdb, animatedProgressRight, updateChartOneRight, modifier= Modifier.weight(1f))
            }
        } else {
            Log.d(TAG, "In row arrangement")
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly
            ) {  // outline
                LeftOneSecView(leftdb, animatedProgressLeft, updateChartOneLeft, modifier= Modifier.weight(1f))
                Spacer(modifier=Modifier.weight(0.1f))
                RightOneSecView(rightdb, animatedProgressRight, updateChartOneRight, modifier= Modifier.weight(1f))
            }
        }


        LaunchedEffect(key1 = Unit) {
            var countToSec = 0  // count to 16,6 = 1 sec
            while (true) {
                if (MeterService.isRecording) {
                    countToSec++
                    if (countToSec >= 60) {
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

                delay(1000/60)  // 60 Hz (refresh rate of the screen)
                // TODO verify if second is used correctly
            }
        }
    }

    @Composable
    fun LeftOneSecView(leftdb: String, progessLeft: Float, updateChartOneLeft: Float, modifier: Modifier = Modifier) {
        Row(modifier = modifier.padding(2.dp)) { // left
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {  // arrangement in column
                Text(text = "Left channel", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = modifier.fillMaxWidth()) {
                    Text(text = "$leftdb dB", modifier= Modifier
                        .padding(2.dp, 0.dp, 0.dp, 0.dp)
                        .weight(1f))
                    LinearProgressIndicator(
                        modifier = modifier
                            .semantics(mergeDescendants = true) {}
                            .requiredHeight(PROGRESS_BAR_HEIGHT)
                            .requiredWidth(PROGRESS_BAR_WIDTH)
                            .weight(2f),
                        progress = progessLeft,
                    )
                }

                Charts.ONE_SEC_LEFT(updateTrigger = updateChartOneLeft, modifier = modifier.fillMaxSize()) // TODO why becomes a scatter chart?
            }
        }
    }

    @Composable
    fun RightOneSecView(rightdb: String, progessRight: Float, updateChartOneRight: Float, modifier: Modifier = Modifier){
        Row(modifier = modifier.padding(2.dp)) { // right
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {  // arrangement in column
                Text(text = "Right channel", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround, modifier = modifier.fillMaxWidth()) {
                    Text(text = "$rightdb dB", modifier= Modifier
                        .padding(2.dp, 0.dp, 0.dp, 0.dp)
                        .weight(1f))
                    LinearProgressIndicator(
                        modifier = modifier
                            .semantics(mergeDescendants = true) {}
                            .requiredHeight(PROGRESS_BAR_HEIGHT)
                            .requiredWidth(PROGRESS_BAR_WIDTH)
                            .weight(1.5f),
                        progress = progessRight,
                    )
                }

                Charts.ONE_SEC_RIGHT(updateTrigger = updateChartOneRight, modifier = modifier.fillMaxSize())
            }
        }
    }


    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    fun FiveMinView(){
        var onUpdateChartFiveLeft by remember { mutableStateOf(0f) }
        var onUpdateChartFiveRight by remember { mutableStateOf(0f) }


        val windowSizeClass = calculateWindowSizeClass(this)
        if (windowSizeClass.heightSizeClass != WindowHeightSizeClass.Compact) {
            Log.d(TAG, "In column arrangement")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp), verticalArrangement = Arrangement.SpaceEvenly
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
                    .padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly
            ) {  // outline
                LeftFiveMinView(onUpdateChartFiveLeft, modifier= Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(0.05f))
                RightFiveMinView(onUpdateChartFiveRight, modifier= Modifier.weight(1f))
            }
        }


        LaunchedEffect(key1 = Unit, block = {
            while (true){
                if(MeterService.isRecording){
                    Log.d(TAG, "Launched effect: FiveMinView")

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
                    .padding(2.dp)) // TODO why becomes a scatter chart?
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
}



