package it.albertopasqualetto.soundmeteresp

import android.util.Log
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

@Suppress("UNUSED_EXPRESSION")
enum class Charts {
    ONE_SEC_LEFT, ONE_SEC_RIGHT,
    FIVE_MIN_LEFT, FIVE_MIN_RIGHT;

    lateinit var chart: LineChart


    @Composable
    operator fun invoke(updateTrigger : Float, modifier: Modifier = Modifier){
        val type = this

        val maxEntries = when(type) {
            ONE_SEC_LEFT, ONE_SEC_RIGHT -> 60
            FIVE_MIN_LEFT, FIVE_MIN_RIGHT -> 60*5
        }

        val colorScheme = colorScheme

        AndroidView(
            modifier = modifier,
            factory = { context ->
                LineChart(context).apply {
                    chart = this
                    chart.setTouchEnabled(false)
                    chart.setDrawGridBackground(false)
                    chart.setDrawBorders(false)
                    chart.setBackgroundColor(colorScheme.background.toArgb())
                    chart.description.isEnabled = false
                    chart.legend.isEnabled = false
                    chart.axisRight.isEnabled = false
                    chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                    chart.xAxis.setDrawLabels(false)
                    chart.axisLeft.textColor = colorScheme.onBackground.toArgb()
                    chart.isFocusable = false
                    chart.isClickable = false
                    chart.isLongClickable = false
                    chart.isDoubleTapToZoomEnabled = false
                    chart.isAutoScaleMinMaxEnabled = true
//                    chart.setViewPortOffsets(0f, 0f, 0f, 0f)
                    chart.axisLeft.axisMinimum = 0f
//                    chart.axisLeft.axisMaximum = 200f
                    chart.xAxis.axisMinimum = 0f
                    chart.xAxis.axisMaximum = maxEntries.toFloat()
                    chart.xAxis.setLabelCount(7, true)
                    chart.setMaxVisibleValueCount(0)


                    val dataSet = LineDataSet(mutableListOf<Entry>(), "") // add entries to dataset
                    dataSet.color = colorScheme.primary.toArgb()
                    dataSet.setDrawCircles(false)
                    dataSet.lineWidth = 3f

                    val lineData = LineData(dataSet)
                    chart.data = lineData

                    redraw()
                }
            },
            update = { chart ->
                updateTrigger  // to trigger recomposition
                // update chart here
//                chart.clear()
                val data: LineData = chart.data
                val set = data.getDataSetByIndex(0)
                var newEntry : Entry? = null
                Log.d(TAG, "Chart: update $type, ${set.entryCount}, $updateTrigger")
                try{
                    newEntry = when (type) {
                        ONE_SEC_LEFT -> Entry(set.entryCount.toFloat(), updateTrigger)
                        ONE_SEC_RIGHT -> Entry(set.entryCount.toFloat(), updateTrigger)
                        FIVE_MIN_LEFT -> Entry(set.entryCount.toFloat(), Values.last5MinDbLeftList[set.entryCount])
                        FIVE_MIN_RIGHT -> Entry(set.entryCount.toFloat(), Values.last5MinDbRightList[set.entryCount])
                    }
                    Log.d(TAG, "Chart: newEntry: $newEntry, $type")
                } catch (e : Exception){
                    Log.d(TAG, "Chart: exception: $e, $type")
                    return@AndroidView
                }

                data.addEntry(newEntry, 0)

                if (set.entryCount > maxEntries){
                    set.removeEntry(0)
                    for (i in 1 until set.entryCount){
                        set.getEntryForIndex(i).x = i - 1f
                    }
                }
//                chart.data = LineData(LineDataSet(data, "Label"))
//                chart.data.notifyDataChanged()
                chart.notifyDataSetChanged()    // let the chart know it's data changed
                chart.invalidate()
                Log.d(TAG, "Chart: updated $type")
            }
        )
    }


    fun redraw(){
        Log.d(TAG, "Chart: redraw $this")
        if (!this::chart.isInitialized)
            return

        val dataSet = chart.data.getDataSetByIndex(0)
        dataSet.clear()

        when (this) {
            ONE_SEC_LEFT -> Values.lastSecDbLeftList.forEachIndexed { index, value -> dataSet.addEntry(Entry(index.toFloat(), value)) }
            ONE_SEC_RIGHT -> Values.lastSecDbRightList.forEachIndexed { index, value -> dataSet.addEntry(Entry(index.toFloat(), value)) }
            FIVE_MIN_LEFT -> Values.last5MinDbLeftList.forEachIndexed { index, value -> dataSet.addEntry(Entry(index.toFloat(), value)) }
            FIVE_MIN_RIGHT -> Values.last5MinDbRightList.forEachIndexed { index, value -> dataSet.addEntry(Entry(index.toFloat(), value)) }
        }

        chart.notifyDataSetChanged()
        chart.invalidate()
    }


    companion object {
        private val TAG = Charts::class.simpleName
    }
}
