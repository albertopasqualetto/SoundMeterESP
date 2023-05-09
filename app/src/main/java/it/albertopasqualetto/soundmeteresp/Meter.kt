@file:JvmName("Meter")
package it.albertopasqualetto.soundmeteresp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.activity.ComponentActivity
import kotlin.math.abs
import kotlin.math.log10


object Meter {
    private val TAG = Meter::class.simpleName

    private const val AUDIO_SOURCE = MediaRecorder.AudioSource.UNPROCESSED
    const val SAMPLE_RATE = 44100
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private const val BUFFER_SIZE_FACTOR = 2    // under load this will guarantee a smooth recording
    val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR


    @SuppressLint("MissingPermission")  // already checked in MainActivity (TODO or move here)
    fun initMeter(activity: ComponentActivity) : AudioRecord {
        val requestPermissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                Log.d(TAG, "Permission granted")
            } else {
                // Explain to the user that the feature is unavailable because
                // the features requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
                Log.d(TAG, "Permission denied 1")
                val dialogBuilder = AlertDialog.Builder(activity)
                dialogBuilder.setTitle("Permission denied")
                dialogBuilder.setMessage("You need to grant the permission to use this app.")
                dialogBuilder.setNeutralButton("Exit") { dialog, which ->
                    activity.finish()
                }
                dialogBuilder.show()
            }
        }

        if (ActivityCompat.checkSelfPermission(
                activity,
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

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.CAMERA))
            { // Show dialog to the user explaining why the permission is required
            }

            Log.d(TAG, "No permissions 2")  // TODO write in string ?
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
//                        return
        }

        return AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE)
    }


    fun readLeftRightMeter(meter: AudioRecord) : List<DoubleArray> {
        var buf : ShortArray = ShortArray(BUFFER_SIZE)
        var readN = 0

        try{
            readN = meter.read(buf, 0, BUFFER_SIZE)
            Log.d(TAG, "readN: $readN")
        }catch (e: Exception){
            Log.d(TAG, e.toString())
        }

        val left = buf.filter { it % 2 == 0 }.map { PCMtoDB(it) }.toDoubleArray()
        val right = buf.filter { it % 2 == 1 }.map { PCMtoDB(it) }.toDoubleArray()

        return listOf(left, right)
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

}