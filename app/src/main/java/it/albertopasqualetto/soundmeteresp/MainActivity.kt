package it.albertopasqualetto.soundmeteresp

import it.albertopasqualetto.soundmeteresp.ui.theme.SoundMeterESPTheme
import android.media.AudioRecord
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.log10

class MainActivity : ComponentActivity() {

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


        var buf : ShortArray = ShortArray(Meter.SAMPLE_RATE)
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

    companion object {
        private val TAG = MainActivity::class.simpleName

        const val DELAY_MS : Long = 1000
        var meter : AudioRecord? = null
    }

    @Preview(showBackground = true)
    @Composable
    fun AppContent(){
        var measuring : Boolean by remember { mutableStateOf(false) }
        var db by remember { mutableStateOf("Waiting dB...") }
        var t by remember { mutableStateOf("Waiting raw...") }
    //    var textSize by remember { mutableStateOf(20.sp) }
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally){
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Measure:")
                Spacer(Modifier.width(8.dp))
                Switch(checked = measuring, onCheckedChange = {measuring = it})
            }
            Text(text = db)
            Text(text = t)

            /*Text(text = t.split(",")[0].replace("[", ""))
            Text(text = t,
                lineHeight = 17.sp,
                fontSize = textSize,
                onTextLayout = { textLayoutResult: TextLayoutResult ->
                val maxCurrentLineIndex: Int = textLayoutResult.lineCount - 1

                if (textLayoutResult.hasVisualOverflow || textLayoutResult.isLineEllipsized(maxCurrentLineIndex)) {
                    textSize = textSize.times(0.9f)
                }
            })*/

        }

        // auto-measure
        LaunchedEffect(key1 = Unit, block = {
            while (true){
                if (!measuring) {
                    delay(DELAY_MS)
                    continue
                }
                val measuredVals = Meter.measureNow(meter!!)
                db = "dB: " + measuredVals[0].toString()
                t = "raw: " + measuredVals[1].toString()
                delay(DELAY_MS)
            }
        })
    }

}

fun PCMtoDB(pcm: Number) : Double {
//    return 20 * log10(abs(pcm.toDouble()) / 32768 /51805.5336 / 20e-6)   // TODO scale? +26?
    return 20 * log10((abs(pcm.toDouble()) /32768) / 20e-6)   // TODO scale? +26?   //TODO HOW TO GET THE CORRECT VALUE??????????
}
/*fun PCMtoDB(samples: ShortArray) : Double {
    var sum = 0.0
    for (sample in samples){
        sum += (abs(sample.toDouble()) / 32768).pow(2)
    }
    Log.d("PCMtoDB", "sum: $sum")
    val rms = sqrt(sum / samples.size)
    if (rms < 1) Log.d("PCMtoDB", "rms: $rms")
    return 20 * log10(rms)
}*/

