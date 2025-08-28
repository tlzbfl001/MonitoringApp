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
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.databinding.FragmentRespirationBinding
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.screen.notification.NotificationFragment
import com.aitronbiz.arron.util.CustomUtil.replaceFragment
import com.aitronbiz.arron.viewmodel.RespirationViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

class RespirationFragment : Fragment() {
    private var _binding: FragmentRespirationBinding? = null
    private val binding get() = _binding!!

    private val vm: RespirationViewModel by viewModels()
    private lateinit var roomsAdapter: RoomsAdapter
    private lateinit var homeId: String
    private lateinit var selectedDate: LocalDate
    private var selectedRoomId: String = "ALL"
    private var isToday = false

    companion object {
        private const val ARG_HOME_ID = "argHomeId"
        private const val ARG_DATE = "argSelectedDate"

        // roomId 제거된 버전
        fun newInstance(homeId: String, date: LocalDate): RespirationFragment {
            return RespirationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_HOME_ID, homeId)
                    putLong(ARG_DATE, date.toEpochDay())
                }
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRespirationBinding.inflate(inflater, container, false)
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
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

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
        binding.btnRealtime.visibility =
            if (selectedDate == LocalDate.now()) View.VISIBLE else View.GONE
        binding.btnRealtime.setOnClickListener {
            replaceFragment(
                parentFragmentManager,
                RealTimeRespirationFragment.newInstance(homeId, selectedRoomId, selectedDate), null
            )
        }

        binding.tvCurrentValue.visibility = if (isToday) View.VISIBLE else View.GONE
        binding.tvCurrentTime.visibility = if (isToday) View.VISIBLE else View.GONE

        // 장소 목록
        binding.rvRooms.layoutManager = GridLayoutManager(requireContext(), 3)
        roomsAdapter = RoomsAdapter(onClick = { r ->
            if (selectedRoomId == r.id) return@RoomsAdapter
            selectedRoomId = r.id

            val token = AppController.prefs.getToken().orEmpty()
            if (token.isNotBlank()) {
                if (selectedRoomId == "ALL") {
                    vm.fetchRespirationDataAll(vm.rooms.value.map { it.id }, selectedDate)
                } else {
                    if (selectedDate == LocalDate.now()) vm.fetchPresence(token, r.id)
                    vm.fetchRespirationData(selectedRoomId, selectedDate)
                }
            }

            roomsAdapter.submit(
                listOf(Room(id = "ALL", name = "전체")) + vm.rooms.value,
                presenceMapForSelectedDate(),
                selectedRoomId,
                isToday
            )
        })
        binding.rvRooms.adapter = roomsAdapter

        // 차트 인덱스 변경
        binding.respChart.setOnIndexChangeListener { idx ->
            vm.selectBar(idx)
        }

        // 알림 뱃지
        viewLifecycleOwner.lifecycleScope.launch {
            vm.checkNotifications { has ->
                binding.badgeDot.visibility = if (has) View.VISIBLE else View.GONE
            }
        }

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

                        val list = listOf(Room(id = "ALL", name = "전체")) + rooms

                        // 초기 선택 상태: 유효하지 않으면 ALL
                        if (!hasRooms) {
                            selectedRoomId = "ALL"
                        } else if (selectedRoomId.isBlank() || (selectedRoomId != "ALL" && rooms.none { it.id == selectedRoomId })) {
                            selectedRoomId = "ALL"
                        }

                        roomsAdapter.submit(
                            list,
                            presenceMapForSelectedDate(),
                            selectedRoomId,
                            isToday
                        )

                        if (token.isNotBlank()) {
                            // 오늘이면 각 방 재실 상태 갱신
                            if (isToday) rooms.forEach { room -> vm.fetchPresence(token, room.id) }
                            // 데이터 로드
                            if (selectedRoomId == "ALL") {
                                vm.fetchRespirationDataAll(rooms.map { it.id }, selectedDate)
                            } else {
                                vm.fetchRespirationData(selectedRoomId, selectedDate)
                            }
                        }
                    }
                }

                // 재실 상태 변화
                launch {
                    vm.presenceByRoomId.collect {
                        roomsAdapter.submit(
                            listOf(Room(id = "ALL", name = "전체")) + vm.rooms.value,
                            presenceMapForSelectedDate(),
                            selectedRoomId,
                            isToday
                        )
                    }
                }

                // 차트 데이터
                launch {
                    vm.chartData.collect { list ->
                        if (list.isEmpty()) {
                            binding.tvNoData.visibility = View.VISIBLE
                            return@collect
                        }
                        binding.tvNoData.visibility = View.GONE

                        val upToIdx = endDrawIndexFor(selectedDate, list.lastIndex)
                        val lastIdxWithData = (upToIdx downTo 0).firstOrNull { list[it].value > 0f } ?: upToIdx

                        val fixedMax = ceil(
                            list.take(upToIdx + 1)
                                .maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
                        ).toInt()

                        vm.selectBar(lastIdxWithData)

                        binding.respChart.setChart(
                            raw = ensure1440(list),
                            selectedDate = selectedDate,
                            selectedIndex = lastIdxWithData,
                            fixedMaxY = fixedMax
                        )
                        binding.respChart.requestLayout()
                        binding.respChart.invalidate()
                    }
                }

                // 현재 호흡수 & 요약
                launch {
                    vm.tick.collect {
                        binding.tvCurrentLabel.visibility = if (isToday) View.VISIBLE else View.GONE
                        binding.tvCurrentValue.visibility = if (isToday) View.VISIBLE else View.GONE
                        binding.tvCurrentTime.visibility = if (isToday) View.VISIBLE else View.GONE

                        if (isToday) {
                            val cur = vm.currentBpm.value
                            binding.tvCurrentValue.text = "${cur.roundToInt()} bpm"

                            val now = LocalTime.now()
                            binding.tvCurrentTime.text = String.format("(%02d:%02d)", now.hour, now.minute)
                        }

                        val s = vm.stats.value
                        binding.tvMin.text = "${s.min} bpm"
                        binding.tvMax.text = "${s.max} bpm"
                        binding.tvAvg.text = "${s.avg} bpm"

                        val list = vm.chartData.value
                        if (list.isNotEmpty()) {
                            val upToIdx = endDrawIndexFor(selectedDate, list.lastIndex)
                            val fixedMax = ceil(
                                list.take(upToIdx + 1).maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
                            ).toInt()

                            val lastIdxWithData = (upToIdx downTo 0).firstOrNull { list[it].value > 0f } ?: upToIdx
                            val selIdx = min(vm.selectedIndex.value, upToIdx).coerceAtLeast(0)
                            val finalSel = if (list.getOrNull(selIdx)?.value ?: 0f > 0f) selIdx else lastIdxWithData

                            binding.respChart.setChart(
                                raw = ensure1440(list),
                                selectedDate = selectedDate,
                                selectedIndex = finalSel,
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

    private fun endDrawIndexFor(date: LocalDate, lastIndex: Int): Int {
        return if (date == LocalDate.now()) {
            val now = LocalTime.now()
            min(lastIndex, now.hour * 60 + now.minute)
        } else lastIndex
    }

    private fun presenceMapWithAll(): Map<String, Boolean> {
        val base = vm.presenceByRoomId.value
        val anyPresent = vm.rooms.value.any { room -> base[room.id] == true }
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
