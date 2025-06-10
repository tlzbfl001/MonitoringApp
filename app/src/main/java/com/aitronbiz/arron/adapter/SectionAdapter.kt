package com.aitronbiz.arron.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.entity.DailyData
import com.aitronbiz.arron.entity.SectionItem
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.view.home.DetailFragment
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.time.DayOfWeek
import java.time.LocalDate
import android.view.Gravity
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.aitronbiz.arron.R
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.OnStartDragListener
import com.aitronbiz.arron.view.home.WeeklyDetailFragment
import java.time.temporal.TemporalAdjusters

class SectionAdapter(
    private val context: Context,
    private var subjectId: Int,
    private var deviceId: Int,
    private var sections: MutableList<SectionItem>,
    private val dragStartListener: OnStartDragListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int = when (sections[position]) {
        is SectionItem.TodayActivity -> 0
        is SectionItem.WeeklyActivity -> 1
        is SectionItem.ResidenceTime -> 2
        is SectionItem.SmartEnergy -> 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> TodayActivityViewHolder(inflater.inflate(R.layout.section_today_activity, parent, false))
            1 -> WeeklyActivityViewHolder(inflater.inflate(R.layout.section_weekly_activity, parent, false))
            2 -> ResidenceTimeViewHolder(inflater.inflate(R.layout.section_residence_time, parent, false))
            3 -> SmartEnergyViewHolder(inflater.inflate(R.layout.section_smart_energy, parent, false))
            else -> throw IllegalArgumentException("Invalid viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = sections[position]) {
            is SectionItem.TodayActivity -> (holder as TodayActivityViewHolder).bind(context, subjectId, deviceId)
            is SectionItem.WeeklyActivity -> (holder as WeeklyActivityViewHolder).bind(context, deviceId)
            is SectionItem.ResidenceTime -> {}
            is SectionItem.SmartEnergy -> (holder as SmartEnergyViewHolder).bind(context)
        }

        // 일반 터치로는 드래그가 되지 않도록 처리
        holder.itemView.setOnTouchListener { _, _ -> false }
    }

    override fun getItemCount(): Int = sections.size

    // 아이템을 이동시키는 메서드
    fun moveItem(from: Int, to: Int) {
        val item = sections.removeAt(from)
        sections.add(to, item)
        notifyItemMoved(from, to)
    }

    // subjectId와 deviceId를 갱신하는 메서드
    fun updateSubjectAndDeviceId(subjectId: Int, deviceId: Int) {
        this.subjectId = subjectId
        this.deviceId = deviceId
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

        fun bind(context: Context, subjectId: Int, deviceId: Int) {
            val dataManager = DataManager.getInstance(context)
            val roomStatus = dataManager.getRoomStatus(deviceId)
            val pct = dataManager.getDailyData(deviceId, LocalDate.now().toString())

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
                val activity = context as? AppCompatActivity
                activity?.let {
                    val bundle = Bundle()
                    bundle.putInt("subjectId", subjectId)
                    bundle.putInt("deviceId", deviceId)
                    replaceFragment2(it.supportFragmentManager, DetailFragment(), bundle)
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

    class WeeklyActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private lateinit var _context: Context
        private lateinit var dataManager: DataManager
        private var startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        private val dayNames = arrayOf("일", "월", "화", "수", "목", "금", "토")
        private val cardView = view.findViewById<CardView>(R.id.cardView)
        private val chart = view.findViewById<ConstraintLayout>(R.id.chartContainer)
        private val ivUpDown = view.findViewById<ImageView>(R.id.ivUpDown)
        private val tvDesc = view.findViewById<TextView>(R.id.tvDesc)

        fun bind(context: Context, deviceId: Int) {
            _context = context
            dataManager = DataManager.getInstance(context)
            loadData(deviceId)

            cardView.setOnClickListener {
                val activity = context as? AppCompatActivity
                activity?.let {
                    val bundle = Bundle()
                    bundle.putInt("deviceId", deviceId)
                    replaceFragment2(activity.supportFragmentManager, WeeklyDetailFragment(), bundle)
                }
            }
        }

        private fun loadData(deviceId: Int) {
            val endDate = startOfWeek.plusDays(6)
            val activities = dataManager.getAllDailyData(deviceId, startOfWeek.toString(), endDate.toString())

            val todayActivity = activities.find {
                LocalDate.parse(it.createdAt).isEqual(LocalDate.now())
            }

            val yesterdayActivity = activities.find {
                LocalDate.parse(it.createdAt).isEqual(LocalDate.now().minusDays(1))
            }

            val todayValue = todayActivity?.activityRate ?: 0
            val yesterdayValue = yesterdayActivity?.activityRate ?: 0

            if (todayValue > yesterdayValue) {
                ivUpDown.setImageResource(R.drawable.ic_up)
                tvDesc.text = "${todayValue - yesterdayValue}%"
            } else if (todayValue < yesterdayValue) {
                ivUpDown.setImageResource(R.drawable.ic_down)
                tvDesc.text = "${yesterdayValue - todayValue}%"
            } else {
                ivUpDown.setImageResource(R.drawable.ic_up)
                tvDesc.text = "0"
            }

            updateChart(activities)
        }

        private fun updateChart(data: List<DailyData>) {
            chart.removeAllViews()
            chart.setPadding(0, 0, 0, 0)
            val map = data.associateBy { it.createdAt }
            val parentId = ConstraintLayout.LayoutParams.PARENT_ID
            val yLabelWidth = dpToPx(0f)
            val chartStartMargin = yLabelWidth + dpToPx(4f)
            val chartHeight = dpToPx(100f)
            val barBottomMargin = dpToPx(20f)
            val topTextMargin = dpToPx(16f)
            val usableHeight = chartHeight - barBottomMargin - topTextMargin

            for (i in 0 until 7) {
                val date = startOfWeek.plusDays(i.toLong())
                val activity = map[date.toString()]
                val value = activity?.activityRate ?: 0
                val label = dayNames[i]
                val barBias = i / 6f

                val barWidth = dpToPx(11f)
                val filledHeight = if (value > 0) (usableHeight * (value / 100f)).toInt() else 0

                val emptyBar = FrameLayout(_context).apply {
                    layoutParams = ConstraintLayout.LayoutParams(barWidth, usableHeight).apply {
                        bottomToBottom = parentId
                        bottomMargin = barBottomMargin
                        startToStart = parentId
                        endToEnd = parentId
                        horizontalBias = barBias
                        marginStart = chartStartMargin
                    }
                    background = ContextCompat.getDrawable(_context, R.drawable.bar_background_empty)
                }

                if (value > 0) {
                    val filledBar = View(_context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            filledHeight
                        ).apply {
                            gravity = Gravity.BOTTOM
                        }
                        background = ContextCompat.getDrawable(_context, R.drawable.bar_background_filled)
                    }
                    emptyBar.addView(filledBar)

                    // 막대 위 텍스트
                    val topText = TextView(_context).apply {
                        text = "$value"
                        textSize = 9.5f
                        setTextColor("#CCCCCC".toColorInt())
                        gravity = Gravity.CENTER
                        layoutParams = ConstraintLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomToBottom = parentId
                            // 항상 emptyBar의 위에 위치하도록 전체 usableHeight 기준으로 계산
                            bottomMargin = usableHeight + barBottomMargin + dpToPx(1f)
                            startToStart = parentId
                            endToEnd = parentId
                            horizontalBias = barBias
                            marginStart = chartStartMargin
                        }
                    }
                    chart.addView(topText)
                }

                chart.addView(emptyBar)

                // 요일 라벨은 아래에 표시
                val labelView = TextView(_context).apply {
                    text = label
                    textSize = 11f
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                    layoutParams = ConstraintLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomToBottom = parentId
                        bottomMargin = dpToPx(2f)
                        startToStart = parentId
                        endToEnd = parentId
                        horizontalBias = barBias
                        marginStart = chartStartMargin
                    }
                }
                chart.addView(labelView)
            }
        }

        private fun dpToPx(dp: Float): Int {
            val density = _context.resources.displayMetrics.density
            return (dp * density).toInt()
        }
    }

    class ResidenceTimeViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class SmartEnergyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var regularTypeface: Typeface? = null
        private var boldTypeface: Typeface? = null
        private val btnTelevision = view.findViewById<ConstraintLayout>(R.id.btnTelevision)
        private val btnAirConditioner = view.findViewById<ConstraintLayout>(R.id.btnAirConditioner)
        private val btnLight = view.findViewById<ConstraintLayout>(R.id.btnLight)
        private val btnMicrowave = view.findViewById<ConstraintLayout>(R.id.btnMicrowave)
        private val energyType1 = view.findViewById<TextView>(R.id.energyType1)
        private val energyType2 = view.findViewById<TextView>(R.id.energyType2)
        private val energyType3 = view.findViewById<TextView>(R.id.energyType3)
        private val energyType4 = view.findViewById<TextView>(R.id.energyType4)
        private val energyStatus1 = view.findViewById<TextView>(R.id.energyStatus1)
        private val energyStatus2 = view.findViewById<TextView>(R.id.energyStatus2)
        private val energyStatus3 = view.findViewById<TextView>(R.id.energyStatus3)
        private val energyStatus4 = view.findViewById<TextView>(R.id.energyStatus4)
        private val iv1 = view.findViewById<ImageView>(R.id.iv1)
        private val iv2 = view.findViewById<ImageView>(R.id.iv2)
        private val iv3 = view.findViewById<ImageView>(R.id.iv3)
        private val iv4 = view.findViewById<ImageView>(R.id.iv4)
        private var onOff1 = false
        private var onOff2 = false
        private var onOff3 = false
        private var onOff4 = false

        fun bind(context: Context) {
            regularTypeface = ResourcesCompat.getFont(context, R.font.noto_sans_kr_regular)
            boldTypeface = ResourcesCompat.getFont(context, R.font.noto_sans_kr_bold)

            btnTelevision.setOnClickListener {
                onOff1 = !onOff1
                switchButtonStyle(onOff1, btnTelevision, iv1, energyType1, energyStatus1)
            }

            btnAirConditioner.setOnClickListener {
                onOff2 = !onOff2
                switchButtonStyle(onOff2, btnAirConditioner, iv2, energyType2, energyStatus2)
            }

            btnLight.setOnClickListener {
                onOff3 = !onOff3
                switchButtonStyle(onOff3, btnLight, iv3, energyType3, energyStatus3)
            }

            btnMicrowave.setOnClickListener {
                onOff4 = !onOff4
                switchButtonStyle(onOff4, btnMicrowave, iv4, energyType4, energyStatus4)
            }
        }

        private fun switchButtonStyle(onOff: Boolean, container: ConstraintLayout, image: ImageView, title: TextView, status: TextView) {
            if(onOff) {
                container.setBackgroundResource(R.drawable.rec_12_gradient)
                image.imageTintList = ColorStateList.valueOf(Color.WHITE)
                title.setTextColor(Color.WHITE)
                status.setTextColor(Color.WHITE)
                title.typeface = boldTypeface
                status.typeface = boldTypeface
                status.text = "사용함"
            }else {
                container.setBackgroundResource(R.drawable.rec_12_border_gradient)
                image.imageTintList = ColorStateList.valueOf(Color.BLACK)
                title.setTextColor(Color.BLACK)
                status.setTextColor("#AAAAAA".toColorInt())
                title.typeface = regularTypeface
                status.typeface = regularTypeface
                status.text = "사용안함"
            }
        }
    }
}