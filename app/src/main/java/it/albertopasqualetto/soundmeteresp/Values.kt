@file:JvmName("Values")
package it.albertopasqualetto.soundmeteresp

import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.log10


object Values {
    private val TAG = Values::class.simpleName

    // new items are added at the end, and when shown in graphs they pop from the beginning
    private var leftQueue : ConcurrentLinkedQueue<Float> = ConcurrentLinkedQueue<Float>()
    private var rightQueue : ConcurrentLinkedQueue<Float> = ConcurrentLinkedQueue<Float>()

    // queue popped element
    var lastLeft : Float = 0f
    var lastRight : Float = 0f

    // when items pop from queue it is added to the list of the last second values
    var lastSecDbLeftList = mutableListOf<Float>()  // max size = 1 sec = MeterService.SAMPLE_RATE / 60
    var lastSecDbRightList = mutableListOf<Float>()  // max size = 1 sec = MeterService.SAMPLE_RATE / 60

    // count the number of items added to the last second lists
    private var leftCount = 0
    private var rightCount = 0

    // when last second lists are full they are emptied and the max value is added to the last 5 min list
    var last5MinDbLeftList = mutableListOf<Float>()  // max size = 5 min = 1*60*5
    var last5MinDbRightList = mutableListOf<Float>()  // max size = 5 min = 1*60*5


    /**
     * Insert just measured values in the queues (in order to be shown).
     *
     * Automatically ignores negative infinity values and resamples to 60Hz.
     *
     * @param leftMeasuredValues left channel measured values
     * @param rightMeasuredValues right channel measured values
     * @param readN raw number of values read from the buffer (returned by [`AudioRecord.read()`)
     */
    fun updateQueues(leftMeasuredValues: FloatArray, rightMeasuredValues: FloatArray, readN: Int) {
        val leftValues = downsampleTo60Hz(leftMeasuredValues.sliceArray(0 until readN/2)).filter { it!=Float.NEGATIVE_INFINITY }     // downsample to 60Hz (from 44100Hz) and take only the read part of the array
        leftQueue.addAll(leftValues)
        val rightValues = downsampleTo60Hz(rightMeasuredValues.sliceArray(0 until readN/2)).filter { it!=Float.NEGATIVE_INFINITY }
        rightQueue.addAll(rightValues)
        Log.d(TAG, "updateQueues: leftQueue size: ${leftQueue.size} rightQueue size: ${rightQueue.size}")
    }

    /**
     * Add a value at the end of the list of last 5 minutes samples (left channel).
     * If the list is full, the first element is removed.
     *
     * @param newMaxLeft new value to add to the list
     */
    private fun updateLast5MinDbLeft(newMaxLeft: Float) {
        while (last5MinDbLeftList.size > 1*60*5) { last5MinDbLeftList.removeFirst() }
        last5MinDbLeftList.add(newMaxLeft)
    }

    /**
     * Add a value at the end of the list of last 5 minutes samples (right channel).
     * If the list is full, the first element is removed.
     *
     * @param newMaxRight new value to add to the list
     */
    private fun updateLast5MinDbRight(newMaxRight: Float) {
        while (last5MinDbRightList.size > 1*60*5) { last5MinDbRightList.removeFirst() }
        last5MinDbRightList.add(newMaxRight)
    }

    /**
     * Get the maximum values of the last second samples (left and right channels).
     *
     * @return array of two values: left and right maximum values
     */
    fun getMaxDbLastSec() : FloatArray {
        val maxLeft = lastSecDbLeftList.maxOrNull() ?: if (lastSecDbLeftList.size == 1) lastSecDbLeftList.first() else 0f
        updateLast5MinDbLeft(maxLeft)
        val maxRight = lastSecDbRightList.maxOrNull() ?: if (lastSecDbRightList.size == 1) lastSecDbRightList.first() else 0f
        updateLast5MinDbRight(maxRight)
//        Log.d(TAG, "getMaxDbLastSec: $maxLeft, $maxRight, sizes: ${lastSecDbLeftList.size}, ${lastSecDbRightList.size}")
        return floatArrayOf(maxLeft, maxRight)
    }

    /**
     * Returns the first element of the queue and add it to the list of already popped elements (left channel).
     * Every second the list is emptied and the maximum value is added to the list of last 5 minutes samples.
     *
     * @return first element of the left queue
     */
    fun getFirstFromQueueLeft() : Float? {
        Log.d(TAG, "getFirstFromQueueLeft: leftQueue size: ${leftQueue.size} rightQueue size: ${rightQueue.size}")
        val out = leftQueue.poll()
        if ( out != null ) {
            leftCount++
            lastSecDbLeftList.add(out)
        }

        if (leftCount >= MeterService.SAMPLE_RATE / 60) {
            last5MinDbLeftList.add(lastSecDbLeftList.maxOrNull() ?: if (lastSecDbLeftList.size == 1) lastSecDbLeftList.first() else 0f)
            leftCount = 0
            lastSecDbLeftList.clear()
        }

        lastLeft = out ?: 0f
//        Log.d(TAG, "getFirstFromQueueLeft: $out")
        return out
    }

    /**
     * Returns the first element of the queue and add it to the list of already popped elements (right channel).
     * Every second the list is emptied and the maximum value is added to the list of last 5 minutes samples.
     *
     * @return first element of the right queue
     */
    fun getFirstFromQueueRight() : Float? {
        val out = rightQueue.poll()
        if ( out != null ) {
            rightCount++
            lastSecDbRightList.add(out)
        }

        if (rightCount >= MeterService.SAMPLE_RATE / 60) {
            last5MinDbRightList.add(lastSecDbRightList.maxOrNull() ?: if (lastSecDbRightList.size == 1) lastSecDbRightList.first() else 0f)
            rightCount = 0
            lastSecDbRightList.clear()
        }

        lastRight = out ?: 0f
//        Log.d(TAG, "getFirstFromQueueLeft: $out")
        return out
    }


    fun resetAll() {
        leftQueue.clear()
        rightQueue.clear()
        lastLeft = 0f
        lastRight = 0f
        lastSecDbLeftList.clear()
        lastSecDbRightList.clear()
        leftCount = 0
        rightCount = 0
        last5MinDbLeftList.clear()
        last5MinDbRightList.clear()
    }


    /**
     * Resample the array to 60Hz (from [MeterService.SAMPLE_RATE]).
     *
     * @param originalArray array to resample
     * @return resampled array
     */
    private fun downsampleTo60Hz(originalArray: FloatArray): FloatArray {
        val originalSampleRate = MeterService.SAMPLE_RATE
        val targetSampleRate = 60
        val downsampleFactor = originalSampleRate / targetSampleRate
        val downsampledArraySize = originalArray.size / downsampleFactor
        val downsampledArray = FloatArray(downsampledArraySize)
//        Log.d(TAG, "downsampleTo60Hz: originalArray size: ${originalArray.size} downsampledArray size: ${downsampledArray.size}")

        for (i in 0 until downsampledArraySize) {
            val originalIndex = (i * downsampleFactor)
            downsampledArray[i] = originalArray[originalIndex]
        }

        return downsampledArray
    }

    /**
     * Convert a PCM value to dB.
     *
     * @param pcm value to convert
     * @return value in dB
     */
    fun pcmToDb(pcm: Number) : Float {
        return 20 * log10( (abs(pcm.toFloat()) /32768) / 20e-6f)   // This value is not calibrated
    }
}