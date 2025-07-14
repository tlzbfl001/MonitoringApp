package com.aitronbiz.arron.adapter

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.entity.SectionItem
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.view.home.DetailFragment
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.time.LocalDate
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.view.home.CustomLineChartView
import com.aitronbiz.arron.view.home.RespirationFragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SectionAdapter(
    private val context: Context,
    private var roomId: Int,
    private var deviceId: Int,
    private var date: LocalDate,
    private var sections: MutableList<SectionItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemViewType(position: Int): Int = when (sections[position]) {
        is SectionItem.TodayActivity -> 0
        is SectionItem.DailyActivity -> 1
        is SectionItem.DailyMission -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> TodayActivityViewHolder(inflater.inflate(R.layout.section_today_activity, parent, false))
            1 -> DailyActivityViewHolder(inflater.inflate(R.layout.section_daily_activity, parent, false))
            2 -> DailyRespirationViewHolder(inflater.inflate(R.layout.section_daily_respiration, parent, false))
            else -> throw IllegalArgumentException("Invalid viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = sections[position]) {
            is SectionItem.TodayActivity ->
                (holder as TodayActivityViewHolder).bind(context, roomId, deviceId, date)
            is SectionItem.DailyActivity ->
                (holder as DailyActivityViewHolder).bind(context, deviceId, date)
            is SectionItem.DailyMission ->
                (holder as DailyRespirationViewHolder).bind(context, deviceId, date)
        }

        holder.itemView.setOnTouchListener { _, _ -> false }
    }

    override fun getItemCount(): Int = sections.size

    // roomId와 deviceId를 갱신하는 메서드
    fun updateRoomAndDeviceId(roomId: Int, deviceId: Int) {
        this.roomId = roomId
        this.deviceId = deviceId
        notifyDataSetChanged()
    }

    // 날짜를 갱신하는 메서드
    fun updateSelectedDate(date: LocalDate) {
        this.date = date
        notifyDataSetChanged()
    }

    // sections 리스트를 갱신하는 메서드
    fun updateSections(newSections: List<SectionItem>) {
        sections = newSections.toMutableList()
        notifyDataSetChanged()
    }

    class TodayActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val btnActivity = view.findViewById<CardView>(R.id.btnActivity)
        private val circularProgress = view.findViewById<CircularProgressBar>(R.id.circularProgress)
        private val progressLabel = view.findViewById<TextView>(R.id.progressLabel)
        private val tvStatus1 = view.findViewById<TextView>(R.id.tvStatus1)
        private val tvStatus2 = view.findViewById<TextView>(R.id.tvStatus2)
        private val tvStatus3 = view.findViewById<TextView>(R.id.tvStatus3)
        private val tvAbsent = view.findViewById<TextView>(R.id.tvAbsent)

        fun bind(context: Context, roomId: Int, deviceId: Int, date: LocalDate) {
            val dataManager = DataManager.getInstance(context)
            val roomStatus = dataManager.getRoomStatus(deviceId)
            val pct = dataManager.getDailyData(deviceId, date.toString())

            // 항상 progress 초기화
            circularProgress.progress = 0f
            circularProgress.setProgressWithAnimation(0f, 1)
            progressLabel.text = "0%"

            if(roomStatus == 1) {
                tvStatus1.visibility = View.VISIBLE
                tvStatus2.visibility = View.VISIBLE
                tvStatus3.visibility = View.VISIBLE
                tvAbsent.visibility = View.GONE

                circularProgress.setProgressWithAnimation(pct.toFloat(), 2000)
                progressLabel.text = "$pct%"

                if(pct == 0) {
                    progressLabel.setTextColor("#DDDDDD".toColorInt())
                    setTextStyle(tvStatus2, tvStatus3, tvStatus1, 2)
                }else {
                    progressLabel.setTextColor(Color.BLACK)
                    when (pct) {
                        in 1..30 -> setTextStyle(tvStatus1, tvStatus2, tvStatus3, 1)
                        in 31..70 -> setTextStyle(tvStatus1, tvStatus3, tvStatus2, 1)
                        else -> setTextStyle(tvStatus2, tvStatus3, tvStatus1, 1)
                    }
                }
            }else {
                tvStatus1.visibility = View.GONE
                tvStatus2.visibility = View.GONE
                tvStatus3.visibility = View.GONE
                tvAbsent.visibility = View.VISIBLE
            }

            btnActivity.setOnClickListener {
                if(deviceId == 0) {
                    Toast.makeText(context, "기기를 먼저 등록해주세요", Toast.LENGTH_SHORT).show()
                }else {
                    val activity = context as? AppCompatActivity
                    activity?.let {
                        val bundle = Bundle()
                        bundle.putInt("roomId", roomId)
                        bundle.putInt("deviceId", deviceId)
                        replaceFragment2(it.supportFragmentManager, DetailFragment(), bundle)
                    }
                }
            }
        }

        private fun setTextStyle(none1: TextView, none2: TextView, active: TextView, type: Int) {
            none1.setTextColor("#DDDDDD".toColorInt())
            none2.setTextColor("#DDDDDD".toColorInt())
            none1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
            none2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)

            when (type) {
                1 -> {
                    active.setTextColor(Color.BLACK)
                    active.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
                }
                else -> {
                    active.setTextColor("#DDDDDD".toColorInt())
                    active.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                }
            }
        }
    }

    class DailyActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val customChart = view.findViewById<CustomLineChartView>(R.id.customChart)
        private val tvNoData = view.findViewById<TextView>(R.id.tvNoData)

        private fun generateData(list: ArrayList<Activity>): List<Float> {
            val hourlyData = MutableList(24) { 0f }
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

            list.forEach { activity ->
                try {
                    val dateTime = LocalDateTime.parse(activity.createdAt, formatter)
                    val hour = dateTime.hour
                    hourlyData[hour] = activity.activity.toFloat()
                } catch (e: Exception) {
                    // 무시
                }
            }

            return hourlyData
        }

        fun bind(context: Context, deviceId: Int, date: LocalDate) {
            val dataManager = DataManager.getInstance(context)
            val data = dataManager.getDailyActivities(deviceId, date.toString())

            if (data.isNotEmpty()) {
                customChart.visibility = View.VISIBLE
                tvNoData.visibility = View.GONE

                val hourlyData = generateData(data)
                customChart.hourlyData = hourlyData
                customChart.invalidate()

            } else {
                customChart.visibility = View.GONE
                tvNoData.visibility = View.VISIBLE
            }
        }
    }

    private class CustomMarkerView(context: Context) : MarkerView(context, R.layout.marker_view1) {
        private val tvContent: TextView = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            tvContent.text = e?.y?.toInt().toString()
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2).toFloat(), -height.toFloat())
        }
    }

    class DailyRespirationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val btnActivity = view.findViewById<CardView>(R.id.btnActivity)
        private val barChart = view.findViewById<BarChart>(R.id.barChart)
        private val entries = ArrayList<BarEntry>()

        fun bind(context: Context, deviceId: Int, date: LocalDate) {
            btnActivity.setOnClickListener {
                val activity = context as? AppCompatActivity
                activity?.let {
                    replaceFragment1(it.supportFragmentManager, RespirationFragment())
                }
            }

            for (minute in 0 until 1440) {
                val value = getDbValueForMinute(minute)
                if (value > 0f) {
                    entries.add(BarEntry(minute.toFloat(), value))
                }
            }

            val dataSet = BarDataSet(entries, "Respiration").apply {
                color = "#4A60FF".toColorInt()
                setDrawValues(false)
            }

            val barData = BarData(dataSet)
            barData.barWidth = 0.4f
            barChart.data = barData

            val markerView = object : MarkerView(barChart.context, R.layout.marker_view1) {
                private val tvContent: TextView = findViewById(R.id.tvContent)
                override fun refreshContent(e: Entry?, highlight: Highlight?) {
                    tvContent.text = "${e?.y?.toInt() ?: ""}"
                    super.refreshContent(e, highlight)
                }

                override fun getOffset(): MPPointF {
                    return MPPointF(-(width / 2).toFloat(), -height.toFloat())
                }
            }
            markerView.chartView = barChart
            barChart.marker = markerView

            val rawMaxY = entries.maxByOrNull { it.y }?.y ?: 40f
            val roundedMaxY = kotlin.math.ceil(rawMaxY / 10f) * 10f

            // X축
            barChart.xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 15f
                isGranularityEnabled = true
                setDrawGridLines(false)
                setDrawAxisLine(true)
                axisLineColor = Color.BLACK
                textColor = Color.BLACK
                textSize = 10f
                yOffset = 6f

                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val totalMinutes = value.toInt()
                        val hours = totalMinutes / 60
                        val minutes = totalMinutes % 60
                        return if (minutes % 15 == 0) {
                            String.format("%02d:%02d", hours, minutes)
                        } else {
                            ""
                        }
                    }
                }
            }

            // Y축
            barChart.axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = roundedMaxY
                granularity = 10f
                isGranularityEnabled = true
                setLabelCount(5, true)

                setDrawGridLines(false)
                setDrawAxisLine(true)
                axisLineColor = Color.BLACK
                textColor = Color.BLACK
                textSize = 10f
                xOffset = 6f

                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt()}회"
                    }
                }
            }

            barChart.axisRight.isEnabled = false

            // 데이터 범위 계산
            val firstMinute = entries.minByOrNull { it.x }?.x?.toInt() ?: 0
            val lastMinute = entries.maxByOrNull { it.x }?.x?.toInt() ?: 0

            val leftPadding = barData.barWidth * 9f
            val rightPadding = barData.barWidth * 7f
            val shiftAmount = barData.barWidth * 0.5f

            barChart.xAxis.axisMinimum = firstMinute.toFloat() + shiftAmount - leftPadding
            barChart.xAxis.axisMaximum = lastMinute.toFloat() + rightPadding

            barChart.setVisibleXRangeMaximum(60f)
            barChart.setExtraOffsets(0f, 0f, 0f, 2f)
            barChart.isDragEnabled = true
            barChart.setScaleEnabled(false)
            barChart.setPinchZoom(false)
            barChart.isDoubleTapToZoomEnabled = false
            barChart.description.isEnabled = false
            barChart.legend.isEnabled = false
            barChart.invalidate()
        }

        private fun getDbValueForMinute(minute: Int): Float {
            return when (minute) {
                in 240..250 -> (10..30).random().toFloat()
                in 260..275 -> (10..30).random().toFloat()
                in 280..287 -> (10..30).random().toFloat()
                in 292..310 -> (10..30).random().toFloat()
                in 320..329 -> (10..30).random().toFloat()
                else -> 0f
            }
        }
    }
}