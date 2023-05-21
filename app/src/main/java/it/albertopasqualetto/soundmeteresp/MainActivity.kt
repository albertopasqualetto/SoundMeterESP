package it.albertopasqualetto.soundmeteresp

// min = 0 dB, max = 200 dB

// import MPAndroidChart

// TODO enable rotation

import android.media.AudioRecord
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabPosition
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.albertopasqualetto.soundmeteresp.ui.theme.SoundMeterESPTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    companion object {
        val TAG = MainActivity::class.simpleName

        const val DELAY_MS : Long = 1000
        var meter : AudioRecord? = null

        private val PROGRESS_BAR_HEIGHT = 50.dp
        private val PROGRESS_BAR_WIDTH = 200.dp

        fun dBToProgress(dB : Float) : Float {
            return (dB.toFloat()/2)/100 // scale from 0dB-200dB to 0-1
        }
    }

    private var recorderThread : Thread? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       /* val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
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
        }*/


        try {
            meter = Meter.initMeter(this)
        } catch (e: Exception) {    // TODO handle no permission
            e.printStackTrace()
        }
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
        recorderThread = null
        if (meter?.state == AudioRecord.STATE_INITIALIZED && meter?.recordingState == AudioRecord.RECORDSTATE_RECORDING) meter?.stop() ?: Log.d(TAG, "onPause: meter is not recording")
    }

    override fun onResume() {
        super.onResume()
        if (meter?.state == AudioRecord.STATE_INITIALIZED && meter?.recordingState == AudioRecord.RECORDSTATE_STOPPED) meter?.startRecording() ?: Log.d(TAG, "onResume: meter is not stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (meter?.state == AudioRecord.STATE_INITIALIZED && meter?.recordingState == AudioRecord.RECORDSTATE_STOPPED) meter?.startRecording() ?: Log.d(TAG, "onDestroy: meter is not stopped")
        if (meter?.state == AudioRecord.STATE_INITIALIZED) meter?.release() ?: Log.d(TAG, "onDestroy: meter is not initialized")
        meter = null
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Preview(showBackground = true)
    @Composable
    fun AppContent() {
        val tabs = listOf("Last second", "5 minutes History")
        val pagerState = rememberPagerState(initialPage = 0)
        val coroutineScope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxWidth()) {
            CenterAlignedTopAppBar(title = { Text("Sound Meter", maxLines = 1, overflow = TextOverflow.Ellipsis) })

            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(text = { Text(title) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = {when (index) {
                            0 -> Icon(Icons.Default.Hearing, contentDescription = "Last second")
                            1 ->Icon(Icons.Default.History, contentDescription = "Last 5 minutes")
                            else -> Icon(Icons.Default.Star, contentDescription = "")
                        }}
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


        // auto-measure
        LaunchedEffect(key1 = Unit, block = {
//            delay(DELAY_MS)
            recorderThread = Thread(RecorderRunnable(), "RecorderRunnable")
            recorderThread!!.start()

        })
    }


    @Composable
    fun OneSecView() {
        var leftdb by remember { mutableStateOf("Waiting left dB...") }
        var rightdb by remember { mutableStateOf("Waiting right dB...") }

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

        var onUpdateOneLeft by remember { mutableStateOf(0) }
        var onUpdateOneRight by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp), verticalArrangement = Arrangement.SpaceEvenly
        ) {  // outline

            Row(modifier = Modifier.weight(1f)) { // left
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {  // arrangement in column
                    LinearProgressIndicator(
                        modifier = Modifier
                            .semantics(mergeDescendants = true) {}
                            .padding(10.dp)
                            .requiredHeight(PROGRESS_BAR_HEIGHT)
                            .requiredWidth(PROGRESS_BAR_WIDTH),
                        progress = animatedProgressLeft,
                    )
                    Text(text = leftdb)

                    Charts.ONE_SEC_LEFT(updated = onUpdateOneLeft, modifier = Modifier.fillMaxSize()) // TODO why becomes a scatter chart?
                }
            }
            Row(modifier = Modifier.weight(1f)) { // right
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {  //arrangement in column
                    LinearProgressIndicator(
                        modifier = Modifier
                            .semantics(mergeDescendants = true) {}
                            .padding(10.dp)
                            .requiredHeight(PROGRESS_BAR_HEIGHT)
                            .requiredWidth(PROGRESS_BAR_WIDTH),
                        progress = animatedProgressRight,
                    )
                    Text(text = rightdb)

                    Charts.ONE_SEC_RIGHT(updated = onUpdateOneRight, modifier = Modifier.fillMaxSize())
                }
            }
        }

        LaunchedEffect(key1 = Unit, block = {
            while (true){
                val (dBLeftMax, dBRightMax) = Values.getMaxDbLastSec()
                Log.d(TAG, "leftMax: $dBLeftMax, rightMax: $dBRightMax")
                leftdb = "left dB: $dBLeftMax"
                progressLeft = dBToProgress(dBLeftMax.toFloat())
                onUpdateOneLeft = (0..1_000_000).random()

                rightdb = "right dB: $dBRightMax"
                progressRight = dBToProgress(dBRightMax.toFloat())
                onUpdateOneRight = (0..1_000_000).random()


                delay(DELAY_MS)
            }
        } )
    }


    @Composable
    fun FiveMinView(){
        var onUpdateFiveLeft by remember { mutableStateOf(0) }
        var onUpdateFiveRight by remember { mutableStateOf(0) }


        Column(modifier = Modifier
            .fillMaxSize()
            .padding(20.dp), verticalArrangement = Arrangement.SpaceEvenly) {  // outline

            Row(modifier = Modifier.weight(1f).padding(2.dp)) { // left
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {  // arrangement in column
                    Text(text = "Left dB of last 5 minutes")
                    Charts.FIVE_MIN_LEFT(updated = onUpdateFiveLeft, modifier = Modifier.fillMaxSize().padding(2.dp)) // TODO why becomes a scatter chart?
                }
            }
            Row(modifier = Modifier.weight(1f).padding(2.dp)) { // right
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {  //arrangement in column
                    Text(text = "Right dB of last 5 minutes")
                    Charts.FIVE_MIN_RIGHT(updated = onUpdateFiveRight, modifier = Modifier.fillMaxSize().padding(2.dp))
                }
            }


        }


        LaunchedEffect(key1 = Unit, block = {
            while (true){
                Log.d(TAG, "Launched effect: FiveMinView")
                onUpdateFiveLeft = (0..1_000_000).random()

                onUpdateFiveRight = (0..1_000_000).random()


                delay(1000)
            }
        })

    }




    private inner class RecorderRunnable : Runnable {
        override fun run() {
            while (true) {
                val measuredVals = Meter.readLeftRightMeter(MainActivity.meter!!)
                Values.updateLastSecDbVec(measuredVals)
            }
        }
    }
}



