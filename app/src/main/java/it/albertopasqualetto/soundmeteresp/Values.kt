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
    var lastSecDbLeftList : MutableList<Float> = mutableListOf<Float>()  // max size = 1 sec = MeterService.SAMPLE_RATE / 60
    var lastSecDbRightList : MutableList<Float> = mutableListOf<Float>()  // max size = 1 sec = MeterService.SAMPLE_RATE / 60
    // count the number of items added to the last second lists
    private var leftCount = 0
    private var rightCount = 0
    // when last second lists are full they are emptied and the max value is added to the last 5 min list
    var last5MinDbLeftList : MutableList<Float> = mutableListOf<Float>()  // max size = 5 min = 1*60*5
    var last5MinDbRightList: MutableList<Float> = mutableListOf<Float>()  // max size = 5 min = 1*60*5


    fun updateQueues(leftMeasuredVals: FloatArray, rightMeasuredVals: FloatArray, readN: Int) {
        val leftVals = downsampleTo60Hz(leftMeasuredVals.sliceArray(0 until readN/2)).filter { it!=Float.NEGATIVE_INFINITY }     // downsample to 60Hz (from 44100Hz) and take only the read part of the array
        leftQueue.addAll(leftVals)
        val rightVals = downsampleTo60Hz(rightMeasuredVals.sliceArray(0 until readN/2)).filter { it!=Float.NEGATIVE_INFINITY }
        rightQueue.addAll(rightVals)
        Log.d(TAG, "updateQueues: leftQueue size: ${leftQueue.size} rightQueue size: ${rightQueue.size}")
    }
    
    private fun updateLast5MinDbLeft(newMaxLeft: Float) {
        while (last5MinDbLeftList.size > 1*60*5) { last5MinDbLeftList.removeFirst() }
        last5MinDbLeftList.add(newMaxLeft)
    }

    private fun updateLast5MinDbRight(newMaxRight: Float) {
        while (last5MinDbRightList.size > 1*60*5) { last5MinDbRightList.removeFirst() }
        last5MinDbRightList.add(newMaxRight)
    }

    fun getMaxDbLastSec() : FloatArray {
        val maxLeft = lastSecDbLeftList.maxOrNull() ?: if (lastSecDbLeftList.size == 1) lastSecDbLeftList.first() else 0f
        updateLast5MinDbLeft(maxLeft)
        val maxRight = lastSecDbRightList.maxOrNull() ?: if (lastSecDbRightList.size == 1) lastSecDbRightList.first() else 0f
        updateLast5MinDbRight(maxRight)
//        Log.d(TAG, "getMaxDbLastSec: $maxLeft, $maxRight, sizes: ${lastSecDbLeftList.size}, ${lastSecDbRightList.size}")
        return floatArrayOf(maxLeft, maxRight)
    }

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


    //TODO maybe do an average
    private fun downsampleTo60Hz(originalArray: FloatArray): FloatArray {
        val originalSampleRate = MeterService.SAMPLE_RATE
        val targetSampleRate = 60
        val downsampleFactor = originalSampleRate / targetSampleRate

        /*val downsampledArray = originalArray
            .toList()
            .windowed(downsampleFactor, downsampleFactor, true)
            .map { window ->
                window.filter { it!=Float.NEGATIVE_INFINITY }
                .average().toFloat() }
            .toFloatArray()*/

        /*return originalArray.asList().chunked(downsampleFactor) { chunk ->
            chunk.filter { it!=Float.NEGATIVE_INFINITY }
            .average().toFloat() }.toFloatArray()*/


        val downsampledArraySize = originalArray.size / downsampleFactor
        val downsampledArray = FloatArray(downsampledArraySize)
//        Log.d(TAG, "downsampleTo60Hz: originalArray size: ${originalArray.size} downsampledArray size: ${downsampledArray.size}")

        /*var acc=0f
        var from=0
        for (i in 0 until downsampledArraySize*downsampleFactor) {
            acc+=originalArray[i]
            if (i%downsampleFactor==0) {
                downsampledArray[from] = acc/downsampleFactor
                acc=0f
                from++
            }
        }*/

        for (i in 0 until downsampledArraySize) { // FUNZIONANTE
            val originalIndex = (i * downsampleFactor)
            downsampledArray[i] = originalArray[originalIndex]
        }

        return downsampledArray
    }

    fun PCMtoDB(pcm: Number) : Float {
        return 20 * log10( (abs(pcm.toFloat()) /32768) / 20e-6f)   // TODO scale? +26?   //TODO HOW TO GET THE CORRECT VALUE??????????
    }
}