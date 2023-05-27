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
    // when items pop from queue it is added to the list of the last second values
    private var lastSecDbLeftList : MutableList<Float> = mutableListOf<Float>()  // max size = 1 sec = MeterService.SAMPLE_RATE / 60
    private var lastSecDbRightList : MutableList<Float> = mutableListOf<Float>()  // max size = 1 sec = MeterService.SAMPLE_RATE / 60
    // count the number of items added to the last second lists
    private var leftCount = 0
    private var rightCount = 0
    // when last second lists are full they are emptied and the max value is added to the last 5 min list
    var last5MinDbLeftList : MutableList<Float> = mutableListOf<Float>()  // max size = 5 min = 1*60*5
    var last5MinDbRightList: MutableList<Float> = mutableListOf<Float>()  // max size = 5 min = 1*60*5


    fun updateQueues(leftMeasuredVals: FloatArray, rightMeasuredVals: FloatArray, readN: Int) {
        val leftVals = downsampleTo60Hz(leftMeasuredVals.sliceArray(0 until readN/2))     // downsample to 60Hz (from 44100Hz) and take only the read part of the array
        leftQueue.addAll(leftVals.toList())
        val rightVals = downsampleTo60Hz(rightMeasuredVals.sliceArray(0 until readN/2))
        rightQueue.addAll(rightVals.toList())
    }
    
    private fun updateLast5MinDbLeft(newMaxLeft: Float) {
        while (last5MinDbLeftList.size >= 1*60*5) { last5MinDbLeftList.removeFirst() }
        last5MinDbLeftList.add(newMaxLeft)
    }

    private fun updateLast5MinDbRight(newMaxRight: Float) {
        while (last5MinDbRightList.size >= 1*60*5) { last5MinDbRightList.removeFirst() }
        last5MinDbRightList.add(newMaxRight)
    }

    fun getMaxDbLastSec() : FloatArray {
        val maxLeft = lastSecDbLeftList.maxOrNull() ?: if (lastSecDbLeftList.size == 1) lastSecDbLeftList.first() else 0f
        updateLast5MinDbLeft(maxLeft)
        val maxRight = lastSecDbRightList.maxOrNull() ?: if (lastSecDbRightList.size == 1) lastSecDbRightList.first() else 0f
        updateLast5MinDbRight(maxRight)
        Log.d(TAG, "getMaxDbLastSec: $maxLeft, $maxRight, sizes: ${lastSecDbLeftList.size}, ${lastSecDbRightList.size}")
        return floatArrayOf(maxLeft, maxRight)
    }

    fun getFirstFromQueueLeft() : Float? {
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
        return out
    }

    private fun downsampleTo60Hz(originalArray: FloatArray): FloatArray {
        val originalSampleRate = 44100
        val targetSampleRate = 60
        val downsampleFactor = originalSampleRate / targetSampleRate

        val downsampledArraySize = originalArray.size / downsampleFactor
        val downsampledArray = FloatArray(downsampledArraySize)

        for (i in 0 until downsampledArraySize) {
            val originalIndex = (i * downsampleFactor).toInt()
            downsampledArray[i] = originalArray[originalIndex]
        }

        return downsampledArray
    }

    fun PCMtoDB(pcm: Number) : Float {
        return 20 * log10( (abs(pcm.toFloat()) /32768) / 20e-6f)   // TODO scale? +26?   //TODO HOW TO GET THE CORRECT VALUE??????????
    }
}