package com.aitronbiz.arron.view.home

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentWeeklyDetailBinding
import com.aitronbiz.arron.entity.DailyData
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class WeeklyDetailFragment : Fragment() {
    private var _binding: FragmentWeeklyDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    private val dayNames = arrayOf("일", "월", "화", "수", "목", "금", "토")
    private var deviceId = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWeeklyDetailBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        dataManager = DataManager.getInstance(requireActivity())

        arguments?.let {
            deviceId = it.getInt("deviceId", 0)
        }

        setupUI()

        return binding.root
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        loadData()
    }

    private fun loadData() {
        val endDate = startOfWeek.plusDays(6)
        val activities = dataManager.getAllDailyData(deviceId, startOfWeek.toString(), endDate.toString())

        // tvWeek: "5월 2주" 형식
        val month = startOfWeek.monthValue
        val weekOfMonth = (startOfWeek.dayOfMonth - 1) / 7 + 1
        binding.tvWeek.text = "${month}월 ${weekOfMonth}주"

        updateChart(activities)
    }

    private fun updateChart(data: List<DailyData>) {
        binding.chartContainer.removeAllViews()
        binding.chartContainer.setPadding(0, 0, 0, 0)

        val map = data.associateBy { it.createdAt }

        val parentId = ConstraintLayout.LayoutParams.PARENT_ID
        val yLabelWidth = dpToPx(0f)
        val chartStartMargin = yLabelWidth + dpToPx(4f)
        val chartHeight = dpToPx(200f)
        val barBottomMargin = dpToPx(20f)
        val topTextMargin = dpToPx(16f)
        val usableHeight = chartHeight - barBottomMargin - topTextMargin
        val fullBarHeight = usableHeight.toInt()

        for (i in 0 until 7) {
            val date = startOfWeek.plusDays(i.toLong())
            val activity = map[date.toString()]
            val value = activity?.activityRate ?: 0
            val label = dayNames[i]
            val barBias = i / 6f

            // 요일 텍스트 (막대 아래)
            val labelView = TextView(requireActivity()).apply {
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
            binding.chartContainer.addView(labelView)

            val barWidth = dpToPx(12f)
            val filledHeight = if (value > 0) (usableHeight * (value / 100f)).toInt() else 0

            // 빈 배경 막대 (항상 100% 높이)
            val emptyBar = FrameLayout(requireActivity()).apply {
                layoutParams = ConstraintLayout.LayoutParams(barWidth, fullBarHeight).apply {
                    bottomToBottom = parentId
                    bottomMargin = barBottomMargin
                    startToStart = parentId
                    endToEnd = parentId
                    horizontalBias = barBias
                    marginStart = chartStartMargin
                }
                background = ContextCompat.getDrawable(requireActivity(), R.drawable.bar_background_empty)
            }

            // 채워진 막대 (값 있을 때만)
            if (value > 0) {
                val filledBar = View(requireActivity()).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        filledHeight
                    ).apply {
                        gravity = Gravity.BOTTOM
                    }
                    background = ContextCompat.getDrawable(requireActivity(), R.drawable.bar_background_filled)
                }
                emptyBar.addView(filledBar)

                // 막대 위 텍스트
                val topText = TextView(requireActivity()).apply {
                    text = "$value"
                    textSize = 10f
                    setTextColor(Color.parseColor("#666666"))
                    gravity = Gravity.CENTER
                    layoutParams = ConstraintLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomToBottom = parentId
                        bottomMargin = filledHeight + barBottomMargin + dpToPx(2f)
                        startToStart = parentId
                        endToEnd = parentId
                        horizontalBias = barBias
                        marginStart = chartStartMargin
                    }
                }
                binding.chartContainer.addView(topText)
            }

            binding.chartContainer.addView(emptyBar)
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = requireActivity().resources.displayMetrics.density
        return (dp * density).toInt()
    }
}