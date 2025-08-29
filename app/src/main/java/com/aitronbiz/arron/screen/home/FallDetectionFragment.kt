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
import com.aitronbiz.arron.adapter.RoomsAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.databinding.FragmentFallDetectionBinding
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.screen.notification.NotificationFragment
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.viewmodel.FallViewModel
import kotlinx.coroutines.*
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.min

class FallDetectionFragment : Fragment() {
    private var _binding: FragmentFallDetectionBinding? = null
    private val binding get() = _binding!!

    private val vm: FallViewModel by viewModels()
    private lateinit var roomsAdapter: RoomsAdapter

    private val homeId: String by lazy { requireArguments().getString(ARG_HOME_ID).orEmpty() }
    private var selectedRoomId: String = "ALL"
    private val selectedDate: LocalDate by lazy {
        LocalDate.ofEpochDay(requireArguments().getLong(ARG_DATE_EPOCH))
    }

    private var elapsedJob: Job? = null
    private var isToday = false
    private var roomsWithFallsLabel: String = "-"
    private var didInitialSelect = false

    companion object {
        private const val ARG_HOME_ID = "argHomeId"
        private const val ARG_DATE_EPOCH = "argDateEpoch"

        fun newInstance(homeId: String, date: LocalDate) =
            FallDetectionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_HOME_ID, homeId)
                    putLong(ARG_DATE_EPOCH, date.toEpochDay())
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFallDetectionBinding.inflate(inflater, container, false)
        isToday = (selectedDate == LocalDate.now())
        return binding.root
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        // 상단 인셋
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarContainer) { view, insets ->
            val top = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            view.updatePadding(top = top)
            insets
        }

        setLastTitleForDate()

        binding.btnBk.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 알림
        binding.btnBell.setOnClickListener {
            val f = NotificationFragment().apply {
                arguments = Bundle().apply {
                    putString("homeId", homeId)
                    putString("roomId", selectedRoomId)
                    putLong("selectedDate", selectedDate.toEpochDay())
                }
            }
            replaceFragment2(parentFragmentManager, f, null)
        }

        binding.tvSelectedDate.text = selectedDate.toString()

        // 장소 목록
        binding.rvRooms.layoutManager = GridLayoutManager(requireContext(), 3)
        roomsAdapter = RoomsAdapter { r ->
            if (selectedRoomId == r.id) return@RoomsAdapter
            selectedRoomId = r.id
            vm.setSelectedRoomId(selectedRoomId)

            if (selectedRoomId == "ALL") {
                vm.fetchFallsData("ALL", selectedDate)
                computeRoomsWithFallsLabel()
            } else {
                vm.fetchFallsData(selectedRoomId, selectedDate)
                if (isToday) vm.fetchPresence(selectedRoomId)
            }

            val presence = presenceMapForAdapter()
            roomsAdapter.submit(
                listOf(Room(id = "ALL", name = "전체")) + vm.rooms.value,
                presence,
                selectedRoomId,
                isToday
            )
        }
        binding.rvRooms.adapter = roomsAdapter

        // 알림 뱃지
        viewLifecycleOwner.lifecycleScope.launch {
            vm.checkNotifications { has ->
                binding.badgeDot.visibility = if (has) View.VISIBLE else View.GONE
            }
        }

        // 초기 로딩
        val token = AppController.prefs.getToken().orEmpty()
        if (token.isNotBlank()) {
            vm.setHomeId(homeId)
            vm.updateSelectedDate(selectedDate)
            if (homeId.isNotBlank()) vm.fetchRooms(homeId)
        }

        // 상태 수집
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 장소 목록 수집
                launch {
                    vm.rooms.collect { rooms ->
                        val show = rooms.isNotEmpty()
                        binding.tvRoomsTitle.visibility = if (show) View.VISIBLE else View.GONE
                        binding.rvRooms.visibility = if (show) View.VISIBLE else View.GONE

                        if (rooms.isNotEmpty()) {
                            if (isToday) {
                                withContext(Dispatchers.IO) {
                                    rooms.map { room ->
                                        async { runCatching { vm.fetchPresence(room.id) }.isSuccess }
                                    }.awaitAll()
                                }
                            }

                            if (!didInitialSelect) {
                                didInitialSelect = true
                                selectedRoomId = "ALL"
                                vm.setSelectedRoomId(selectedRoomId)

                                // 낙상 + 호흡 데이터 초기 로딩
                                vm.fetchFallsData("ALL", selectedDate)
                                computeRoomsWithFallsLabel()
                                vm.fetchRespirationDataAll(rooms.map { it.id }, selectedDate)
                            }
                        }

                        val list = listOf(Room(id = "ALL", name = "전체")) + rooms
                        val presence = presenceMapForAdapter()
                        roomsAdapter.submit(list, presence, selectedRoomId, isToday)
                    }
                }

                // 재실 상태 수집
                launch {
                    vm.presenceByRoomId.collect {
                        val presence = presenceMapForAdapter()
                        roomsAdapter.submit(
                            listOf(Room(id = "ALL", name = "전체")) + vm.rooms.value,
                            presence,
                            selectedRoomId,
                            isToday
                        )
                    }
                }

                // 차트 데이터 수집
                launch {
                    vm.chartPoints.collect { points ->
                        val upToIdx = endDrawIndexFor(selectedDate, 1439)
                        val hasAny = points.take(upToIdx + 1).any { it.value > 0f }

                        // 낙상 미발생 안내 토글
                        binding.tvNoDetect.visibility = if (hasAny) View.GONE else View.VISIBLE

                        val allTimes: List<String> = points
                            .take(upToIdx + 1)
                            .filter { it.value > 0f }
                            .map { it.timeLabel }

                        binding.tvLastFallTime.apply {
                            isSingleLine = false
                            maxLines = Int.MAX_VALUE
                            text = if (allTimes.isEmpty()) "-" else allTimes.joinToString("\n")
                        }

                        val last = points.lastOrNull { it.value > 0f }
                        if (last != null) {
                            // binding.tvLastFallTime.text = last.timeLabel
                        } else {
                            binding.tvElapsed.text = "-"
                        }

                        // 장소 라벨
                        val total = points.count { it.value > 0f }
                        binding.tvCount.text = total.toString()
                        binding.tvRoomId.text = when (selectedRoomId) {
                            "ALL" -> if (total > 0) roomsWithFallsLabel else "-"
                            else  -> if (total > 0) roomNameFor(selectedRoomId) else "-"
                        }

                        // 최근 30분 이내 배너
                        if (isToday && last != null) {
                            val lastDateTime = LocalDateTime.of(selectedDate, LocalTime.parse(last.timeLabel))
                            val within30 = Duration.between(lastDateTime, LocalDateTime.now()).toMinutes() in 0..29
                            binding.imgFall.visibility = if (within30) View.VISIBLE else View.GONE
                            binding.tvDangerBanner.visibility = if (within30) View.VISIBLE else View.GONE
                        } else {
                            binding.imgFall.visibility = View.GONE
                            binding.tvDangerBanner.visibility = View.GONE
                        }
                    }
                }

                // 낙상 이후 경과시간
                launch {
                    vm.lastFallInstant.collect { inst ->
                        elapsedJob?.cancel()
                        if (inst == null) {
                            binding.tvElapsed.text = "-"
                        } else {
                            if (isToday) {
                                // 오늘: 실시간 갱신
                                binding.tvElapsed.text = formatElapsedFrom(inst) // 초기 1회
                                restartElapsedTicker(inst) // 1초 간격 업데이트
                            } else {
                                // 과거
                                binding.tvElapsed.text = formatElapsedFromToDayEnd(inst, selectedDate)
                            }
                        }
                    }
                }

                // 호흡 차트: 인덱스 콜백
                launch {
                    binding.respChart.setOnIndexChangeListener { idx ->
                        vm.selectRespIndex(idx)
                    }
                }

                // 호흡 차트 데이터 수집
                launch {
                    vm.respChartData.collect { list ->
                        if (list.isEmpty()) {
                            // 호흡 데이터 없을 때도 차트는 비워 둠
                            return@collect
                        }

                        val upToIdx = endDrawIndexFor(selectedDate, list.lastIndex)

                        val fixedMax = kotlin.math.ceil(
                            list.take(upToIdx + 1)
                                .maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
                        ).toInt()

                        val nowSel = min(vm.respSelectedIndex.value, upToIdx).coerceAtLeast(0)

                        binding.respChart.setChart(
                            raw = ensure1440(list),
                            selectedDate = selectedDate,
                            selectedIndex = nowSel,
                            fixedMaxY = fixedMax
                        )
                        binding.respChart.invalidate()
                    }
                }

                // 1분 틱
                launch {
                    vm.respTick.collect {
                        val list = vm.respChartData.value
                        if (list.isNotEmpty()) {
                            val upToIdx = endDrawIndexFor(selectedDate, list.lastIndex)

                            val fixedMax = kotlin.math.ceil(
                                list.take(upToIdx + 1)
                                    .maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
                            ).toInt()

                            val nowSel = min(vm.respSelectedIndex.value, upToIdx).coerceAtLeast(0)

                            binding.respChart.setChart(
                                raw = ensure1440(list),
                                selectedDate = selectedDate,
                                selectedIndex = nowSel,
                                fixedMaxY = fixedMax
                            )
                            binding.respChart.invalidate()
                        }
                    }
                }
            }
        }
    }

    private fun ensure1440(list: List<ChartPoint>): List<ChartPoint> {
        if (list.size >= 1440) return list
        val out = ArrayList<ChartPoint>(1440)
        out.addAll(list)
        for (i in list.size until 1440) {
            val h = i / 60
            val m = i % 60
            out.add(ChartPoint(String.format("%02d:%02d", h, m), 0f))
        }
        return out
    }

    // 오늘이면 현재 시각까지, 과거는 끝까지
    private fun endDrawIndexFor(date: LocalDate, lastIndex: Int): Int {
        return if (date == LocalDate.now()) {
            val now = LocalTime.now()
            min(lastIndex, now.hour * 60 + now.minute)
        } else lastIndex
    }

    private fun formatElapsedFrom(inst: Instant): String {
        val d = Duration.between(inst, Instant.now())
        val secs = d.seconds.coerceAtLeast(0)
        return if (secs >= 3600) formatHMS(d) else formatMS(d)
    }

    private fun formatElapsedFromToDayEnd(inst: Instant, date: LocalDate): String {
        val zone = ZoneId.systemDefault()
        val endOfDayExclusive = date.plusDays(1).atStartOfDay(zone).toInstant()
        val endOfDayInclusive = endOfDayExclusive.minusSeconds(1)
        val d = Duration.between(inst, endOfDayInclusive)
        val secs = d.seconds.coerceAtLeast(0)
        return if (secs >= 3600) formatHMS(d) else formatMS(d)
    }

    private fun formatHMS(d: Duration): String {
        var secs = d.seconds
        if (secs < 0) secs = 0
        val h = secs / 3600
        val m = (secs % 3600) / 60
        val s = secs % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    private fun formatMS(d: Duration): String {
        var secs = d.seconds
        if (secs < 0) secs = 0
        val m = secs / 60
        val s = secs % 60
        return String.format("%02d:%02d", m, s)
    }

    private fun roomNameFor(roomId: String): String {
        if (roomId == "ALL") return "전체"
        return vm.rooms.value.firstOrNull { it.id == roomId }?.name ?: "장소"
    }

    private fun presenceMapForAdapter(): Map<String, Boolean> {
        if (!isToday) return emptyMap()
        val base = vm.presenceByRoomId.value
        val anyPresent = vm.rooms.value.any { room -> base[room.id] == true }
        return base.toMutableMap().apply { put("ALL", anyPresent) }
    }

    private fun computeRoomsWithFallsLabel() {
        roomsWithFallsLabel = "-"
        binding.tvRoomId.text = roomsWithFallsLabel

        val rooms = vm.rooms.value
        if (rooms.isEmpty()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val names = withContext(Dispatchers.IO) {
                val zone = ZoneId.systemDefault()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .withZone(ZoneId.of("UTC"))
                val start = selectedDate.atStartOfDay(zone).toInstant()
                val end = selectedDate.plusDays(1).atStartOfDay(zone).toInstant()

                rooms.map { room ->
                    async {
                        runCatching {
                            val res = RetrofitClient.apiService.getFalls(
                                token = "Bearer ${AppController.prefs.getToken()}",
                                roomId = room.id,
                                startTime = formatter.format(start),
                                endTime = formatter.format(end)
                            )
                            if (res.isSuccessful) {
                                val alerts = res.body()?.alerts.orEmpty()
                                val count = alerts.mapNotNull { a ->
                                    runCatching { Instant.parse(a.detectedAt) }.getOrNull()
                                }.count { it >= start && it < end }
                                if (count > 0) room.name else null
                            } else null
                        }.getOrNull()
                    }
                }.awaitAll().filterNotNull()
            }

            roomsWithFallsLabel = if (names.isEmpty()) "-" else names.joinToString(" ")
            if (selectedRoomId == "ALL") {
                binding.tvRoomId.text = roomsWithFallsLabel
            }
        }
    }

    // 카드 라벨 텍스트: 오늘/과거에 따라 변경
    private fun setLastTitleForDate() {
        val label = "낙상 발생시간"
        binding.labelLastFallTime.text = label
    }

    // 오늘만 실시간 틱커 동작
    private fun restartElapsedTicker(inst: Instant?) {
        elapsedJob?.cancel()

        if (!isToday) {
            return
        }

        if (inst == null) {
            binding.tvElapsed.text = "-"
            return
        }

        elapsedJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    binding.tvElapsed.text = formatElapsedFrom(inst)
                    delay(1_000L)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        elapsedJob?.cancel()
        _binding = null
    }
}
