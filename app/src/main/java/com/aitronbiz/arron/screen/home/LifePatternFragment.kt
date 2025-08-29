package com.aitronbiz.arron.screen.home

import android.os.Bundle
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aitronbiz.arron.databinding.FragmentLifePatternBinding
import com.aitronbiz.arron.screen.notification.NotificationFragment
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.viewmodel.LifePatternsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

class LifePatternFragment : Fragment() {
    private var _binding: FragmentLifePatternBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_HOME_ID = "argHomeId"
        private const val ARG_DATE_EPOCH = "argDateEpoch"

        fun newInstance(homeId: String, date: LocalDate) =
            LifePatternFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_HOME_ID, homeId)
                    putLong(ARG_DATE_EPOCH, date.toEpochDay())
                }
            }
    }

    private val viewModel: LifePatternsViewModel by viewModels()
    private val homeId: String by lazy { arguments?.getString(ARG_HOME_ID).orEmpty() }
    private val selectedDate: LocalDate by lazy {
        LocalDate.ofEpochDay(arguments?.getLong(ARG_DATE_EPOCH) ?: LocalDate.now().toEpochDay())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLifePatternBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarContainer) { view, insets ->
            val top = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            view.updatePadding(top = top)
            insets
        }

        binding.btnBk.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnBell.setOnClickListener {
            val f = NotificationFragment().apply {
                arguments = Bundle().apply {
                    putString("homeId", homeId)
                    putLong("selectedDate", selectedDate.toEpochDay())
                }
            }
            replaceFragment2(parentFragmentManager, f, null)
        }

        // 날짜 표기
        binding.tvSelectedDate.text = selectedDate.toString()

        // 데이터 로드
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resetState(homeId)
            viewModel.fetchLifePatternsData(homeId, selectedDate)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.lifePatterns.collect { lp ->
                        if (lp != null) {
                            binding.tvTotalActive.text   = formatMinutes(lp.totalActiveMinutes)
                            binding.tvTotalInactive.text = formatMinutes(lp.totalInactiveMinutes)
                            binding.tvAvgScore.text      = "${lp.averageActivityScore.toInt()}점"
                            binding.tvMaxScore.text      = "${lp.maxActivityScore}점"

                            binding.tvFirstActivity.text = hhmm(lp.firstActivityTime)
                            binding.tvLastActivity.text  = hhmm(lp.lastActivityTime)
                            binding.tvSleepMinutes.text  = formatMinutes(lp.estimatedSleepMinutes)
                            binding.tvSleepRange.text    = run {
                                val s = hhmm(lp.estimatedSleepStart)
                                val e = hhmm(lp.estimatedSleepEnd)
                                if (s == "정보 없음" || e == "정보 없음") "정보 없음" else "$s ~ $e"
                            }
                            binding.tvMostActiveHour.text  = "${lp.mostActiveHour}시"
                            binding.tvLeastActiveHour.text = "${lp.leastActiveHour}시"
                            binding.tvPatternType.text     = patternKo(lp.activityPatternType)
                            binding.tvRegularityScore.text = "${lp.activityRegularityScore.toInt()}점"
                        }
                    }
                }

                // 알림 배지
                launch {
                    viewModel.checkNotifications { has ->
                        binding.badgeDot.visibility = if (has) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun formatMinutes(mins: Int?): String {
        val m = mins ?: 0
        val h = m / 60
        val r = m % 60
        return when {
            h == 0 && r == 0 -> "0분"
            h > 0 && r == 0  -> "${h}시간"
            h > 0            -> "${h}시간 ${r}분"
            else             -> "${r}분"
        }
    }

    private fun hhmm(utc: String?): String = try {
        if (utc.isNullOrBlank()) "정보 없음" else {
            val t = java.time.Instant.parse(utc)
                .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
            if (t.minute == 0) "${t.hour}시" else "${t.hour}시 ${t.minute}분"
        }
    } catch (_: Exception) { "정보 없음" }

    private fun patternKo(s: String?): String = when (s?.lowercase()) {
        "regular" -> "규칙적"
        "irregular" -> "불규칙적"
        "night_owl" -> "야간형"
        "early_bird" -> "주간형"
        "inactive" -> "저활동적"
        else -> "정보 없음"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}