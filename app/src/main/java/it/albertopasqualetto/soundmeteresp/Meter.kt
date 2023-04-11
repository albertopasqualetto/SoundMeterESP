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


object Meter {
    private val TAG = Meter::class.simpleName

    const val SAMPLE_RATE = 44100
    val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)

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
            Log.d(TAG, "No permissions 2")
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
//                        return
        }

        return AudioRecord(MediaRecorder.AudioSource.UNPROCESSED, SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE)
    }

    fun measureNow(meter: AudioRecord) : List<Any> {
        val buf = soundMeter(meter)

        /*var str : String = "[ "
        for (j in buf)
            str += "$j, "
        str += "]"*/

        val mean = buf.average()

//    val dB = PCMtoDB(buf[0])
        val dB = PCMtoDB(mean)
//    Log.d("measureNow", str)
        Log.d(TAG, "raw: $mean")
        Log.d(TAG, "dB: $dB")
        return listOf(dB, mean)

        // TODO separate left and right channels
    }

    fun soundMeter(meter: AudioRecord) : ShortArray {
        var buf : ShortArray = ShortArray(BUFFER_SIZE)
        var readN = 0

        try{
            readN = meter.read(buf, 0, BUFFER_SIZE)
            Log.d(TAG, "readN: $readN")
        }catch (e: Exception){
            Log.d(TAG, e.toString())
        }
        return buf
    }

    /*companion object {
        private val TAG = Meter::class.simpleName

        const val SAMPLE_RATE = 44100
        val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
    }*/


}