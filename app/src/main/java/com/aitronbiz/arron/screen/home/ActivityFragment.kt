package com.aitronbiz.arron.screen.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.databinding.FragmentActivityBinding
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.adapter.RoomsAdapter
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.screen.notification.NotificationFragment
import com.aitronbiz.arron.util.CustomUtil.replaceFragment
import com.aitronbiz.arron.viewmodel.ActivityViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

class ActivityFragment : Fragment() {
    private var _binding: FragmentActivityBinding? = null
    private val binding get() = _binding!!

    private val vm: ActivityViewModel by viewModels()
    private lateinit var roomsAdapter: RoomsAdapter

    private lateinit var homeId: String
    private lateinit var selectedDate: LocalDate
    private var didInitialSelect = false
    private var currentSelectedIdx: Int = -1
    private var selectedRoomId: String = "ALL"
    private var isToday = false

    companion object {
        private const val ARG_HOME_ID = "argHomeId"
        private const val ARG_DATE = "argSelectedDate"

        fun newInstance(homeId: String, date: LocalDate) =
            ActivityFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_HOME_ID, homeId)
                    putLong(ARG_DATE, date.toEpochDay())
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        homeId = args.getString(ARG_HOME_ID).orEmpty()
        selectedDate = LocalDate.ofEpochDay(args.getLong(ARG_DATE))
        isToday = (selectedDate == LocalDate.now())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        // 상태바 패딩
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
                    putString("roomId", selectedRoomId)
                    putLong("selectedDate", selectedDate.toEpochDay())
                }
            }
            replaceFragment(parentFragmentManager, f, null)
        }

        binding.tvSelectedDate.text = selectedDate.toString()
        if(!isToday) {
            binding.tvCurrentLabel.visibility = View.GONE
            binding.tvCurrentTime.visibility = View.GONE
            binding.tvCurrent.visibility = View.GONE
        }

        // 장소 목록
        binding.rvRooms.layoutManager = GridLayoutManager(requireContext(), 3)
        roomsAdapter = RoomsAdapter(onClick = { room ->
            if (selectedRoomId == room.id) return@RoomsAdapter
            selectedRoomId = room.id

            val token = AppController.prefs.getToken().orEmpty()
            if (token.isNotBlank()) {
                refreshDataForSelection(token) // 선택 변경 시마다 데이터 갱신
            }

            // 어댑터 선택상태 갱신
            roomsAdapter.submit(
                listOf(Room(id = "ALL", name = "전체")) + vm.rooms.value,
                presenceMapForSelectedDate(),
                selectedRoomId,
                isToday
            )
        })
        binding.rvRooms.adapter = roomsAdapter

        // 차트 선택 인덱스 변경 콜백
        binding.activityChart.setOnIndexChangeListener { idx ->
            currentSelectedIdx = idx          // 현재 선택 인덱스 기억
            vm.selectBar(idx)
        }

        // 알림 뱃지
        viewLifecycleOwner.lifecycleScope.launch {
            vm.checkNotifications { has ->
                binding.badgeDot.visibility = if (has) View.VISIBLE else View.GONE
            }
        }

        // 초기 데이터 로드
        val token = AppController.prefs.getToken().orEmpty()
        if (token.isNotBlank()) {
            vm.updateSelectedDate(selectedDate)
            if (homeId.isNotBlank()) vm.fetchRooms(token, homeId)
        }

        // 상태 수집
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 방 목록
                launch {
                    vm.rooms.collect { rooms ->
                        val hasRooms = rooms.isNotEmpty()
                        val visible = if (hasRooms) View.VISIBLE else View.GONE
                        binding.rvRooms.visibility = visible
                        binding.tvRoomsTitle.visibility = visible

                        // 유효하지 않은 roomId면 ALL로 회귀
                        if (!hasRooms) {
                            selectedRoomId = "ALL"
                        } else if (selectedRoomId.isBlank() || (selectedRoomId != "ALL" && rooms.none { it.id == selectedRoomId })) {
                            selectedRoomId = "ALL"
                        }

                        roomsAdapter.submit(
                            listOf(Room(id = "ALL", name = "전체")) + rooms,
                            presenceMapForSelectedDate(),
                            selectedRoomId,
                            isToday
                        )

                        // 오늘이면 각 방 재실 상태 즉시 갱신
                        if (token.isNotBlank() && isToday) {
                            rooms.forEach { room -> vm.fetchPresence(token, room.id) }
                        }

                        // 이후 데이터 로드
                        if (token.isNotBlank()) {
                            refreshDataForSelection(token)
                        }
                    }
                }

                // 재실 상태 변경 -> 칩 UI 반영
                launch {
                    vm.presenceByRoomId.collect { map ->
                        roomsAdapter.submit(
                            listOf(Room(id = "ALL", name = "전체")) + vm.rooms.value,
                            presenceMapForSelectedDate(),
                            selectedRoomId,
                            isToday
                        )
                    }
                }

                // 차트 + 요약
                launch {
                    vm.chartData.collect { rawList ->
                        val buckets = aggregateTo10Min(rawList)
                        val upToIdx = endDrawIndexFor(selectedDate)

                        // 현재 시점까지 값이 하나라도 있는지
                        val hasAny = buckets.take(upToIdx + 1).any { it.value > 0f }
                        // 마지막 유효 데이터 인덱스(없으면 -1)
                        val lastIdxWithData = if (hasAny)
                            (upToIdx downTo 0).first { buckets[it].value > 0f }
                        else
                            -1

                        // 눈금 고정: 현재 시점까지의 최대값 기준
                        val fixedMax = ceil(
                            buckets.take(upToIdx + 1).maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
                        ).toInt()

                        // 최초 진입 시: 값이 있을 때만 자동 선택
                        val selectedForSetChart = when {
                            !didInitialSelect && hasAny -> {
                                didInitialSelect = true
                                currentSelectedIdx = lastIdxWithData
                                lastIdxWithData
                            }
                            currentSelectedIdx in 0..upToIdx -> currentSelectedIdx
                            hasAny -> lastIdxWithData
                            else -> -1 // 아직 값 없음 → 툴팁 표시 안 함
                        }

                        // ViewModel에도 반영(필요시)
                        if (selectedForSetChart >= 0) vm.selectBar(selectedForSetChart)

                        // 차트 갱신(선택 인덱스 전달)
                        binding.activityChart.setChart(
                            raw = buckets,
                            selectedDate = selectedDate,
                            selectedIndex = selectedForSetChart,
                            fixedMaxY = fixedMax
                        )
                        binding.activityChart.invalidate()

                        // 요약 지표: 선택 인덱스 기준으로 현재값 계산
                        val slice = buckets.take(upToIdx + 1).map { it.value }
                        val current = if (selectedForSetChart in 0..upToIdx)
                            buckets[selectedForSetChart].value.roundToInt()
                        else
                            0
                        val nonZero = slice.filter { it > 0f }
                        val avg = if (nonZero.isNotEmpty()) nonZero.average().roundToInt() else 0
                        val minVal = if (nonZero.isNotEmpty()) nonZero.minOrNull()!!.roundToInt() else 0
                        val maxVal = slice.maxOrNull()?.roundToInt() ?: 0

                        binding.tvCurrent.text = current.toString()
                        binding.tvAvg.text = avg.toString()
                        binding.tvMin.text = minVal.toString()
                        binding.tvMax.text = maxVal.toString()
                    }
                }
            }
        }
    }

    private fun refreshDataForSelection(token: String) {
        if (selectedRoomId == "ALL") {
            val roomIds = vm.rooms.value.map { it.id }
            if (roomIds.isNotEmpty()) {
                vm.fetchActivityDataAll(token, roomIds, selectedDate)
            } else {
                vm.clearChart()
            }
        } else {
            if (isToday) vm.fetchPresence(token, selectedRoomId)
            vm.fetchActivityData(token, selectedRoomId, selectedDate)
        }
    }

    // 10분 버킷 합산
    private fun aggregateTo10Min(list: List<ChartPoint>): List<ChartPoint> {
        val acc = FloatArray(144) { 0f }
        list.forEach { p ->
            runCatching {
                val (h, m) = p.timeLabel.split(":").map(String::toInt)
                val idx = (h * 60 + m) / 10
                if (idx in 0..143) acc[idx] += p.value
            }
        }
        return List(144) { i ->
            val totalM = i * 10
            val h = totalM / 60
            val mm = totalM % 60
            ChartPoint(String.format("%02d:%02d", h, mm), acc[i])
        }
    }

    private fun endDrawIndexFor(date: LocalDate): Int {
        return if (date == LocalDate.now()) {
            val now = LocalTime.now()
            min(143, (now.hour * 60 + now.minute) / 10)
        } else 143
    }

    private fun presenceMapWithAll(): Map<String, Boolean> {
        val base = vm.presenceByRoomId.value
        val anyPresent = vm.rooms.value.any { room -> base[room.id] == true }
        // ALL 카드에 집계 상태 부여
        return base.toMutableMap().apply { put("ALL", anyPresent) }
    }

    private fun presenceMapForSelectedDate(): Map<String, Boolean> {
        return if (isToday) presenceMapWithAll() else emptyMap()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
