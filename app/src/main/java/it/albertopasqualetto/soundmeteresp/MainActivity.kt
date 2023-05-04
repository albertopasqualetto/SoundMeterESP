package it.albertopasqualetto.soundmeteresp

// min = 0 dB, max = 200 dB

import it.albertopasqualetto.soundmeteresp.ui.theme.SoundMeterESPTheme
import android.media.AudioRecord
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.log10

class MainActivity : ComponentActivity() {
    companion object {
        private val TAG = MainActivity::class.simpleName

        const val DELAY_MS : Long = 1000
        var meter : AudioRecord? = null

        private val PROGRESS_BAR_HEIGHT = 50.dp
        private val PROGRESS_BAR_WIDTH = 200.dp

        fun dBToProgress(dB : Double) : Float {
            return (dB.toFloat()/2)/100 // scale from 0dB-200dB to 0-1
        }
    }

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


        var buf = ShortArray(Meter.BUFFER_SIZE)
//        meter = AudioRecord(MediaRecorder.AudioSource.UNPROCESSED, Meter.SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, Meter.BUFFER_SIZE)
//        meter.startRecording()
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

    @Preview(showBackground = true)
    @Composable
    fun AppContent(){
        var measuring : Boolean by remember { mutableStateOf(false) }
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

    //    var textSize by remember { mutableStateOf(20.sp) }
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally){
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Measure:")
                Spacer(Modifier.width(8.dp))
                Switch(checked = measuring, onCheckedChange = {measuring = it})
            }

            Spacer(Modifier.requiredHeight(50.dp))

            Row(modifier=Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier=Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .semantics(mergeDescendants = true) {}
                            .padding(10.dp)
                            .rotate(-90f)
                            .requiredHeight(PROGRESS_BAR_HEIGHT)
                            .requiredWidth(PROGRESS_BAR_WIDTH),
                        progress = animatedProgressLeft,
                    )
                    Text(text = leftdb)
                }
                Column(modifier=Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .semantics(mergeDescendants = true) {}
                            .padding(10.dp)
                            .rotate(-90f)
                            .requiredHeight(PROGRESS_BAR_HEIGHT)
                            .requiredWidth(PROGRESS_BAR_WIDTH),
                        progress = animatedProgressRight,
                    )
                    Text(text = rightdb)
                }
            }



        }

        // auto-measure
        LaunchedEffect(key1 = Unit, block = {
            while (true){
                if (!measuring) {
                    delay(DELAY_MS)
                    continue
                }
                val measuredVals = Meter.measureNow(meter!!)
                leftdb = "left dB: " + measuredVals[0].toInt().toString()
                progressLeft = dBToProgress(measuredVals[0])

                rightdb = "right dB: " + measuredVals[1].toInt().toString()
                progressRight = dBToProgress(measuredVals[1])
                delay(DELAY_MS)
            }
        })
    }
}



