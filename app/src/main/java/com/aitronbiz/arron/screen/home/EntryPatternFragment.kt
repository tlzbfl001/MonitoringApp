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
import com.aitronbiz.arron.databinding.FragmentEntryPatternBinding
import com.aitronbiz.arron.screen.notification.NotificationFragment
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.viewmodel.EntryPatternsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

class EntryPatternFragment : Fragment() {
    private var _binding: FragmentEntryPatternBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EntryPatternsViewModel by viewModels()
    private lateinit var roomsAdapter: RoomsAdapter
    private var selectedRoomId: String = "ALL"
    private val homeId: String by lazy { arguments?.getString("homeId") ?: "" }
    private val selectedDate: LocalDate by lazy {
        val epoch = arguments?.getLong("selectedDate") ?: LocalDate.now().toEpochDay()
        LocalDate.ofEpochDay(epoch)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEntryPatternBinding.inflate(inflater, container, false)
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

        binding.btnBk.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        // 날짜 표시
        binding.tvSelectedDate.text = selectedDate.toString()

        // 알림 배지
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.checkNotifications { has ->
                binding.badgeDot.visibility = if (has) View.VISIBLE else View.GONE
            }
        }
        binding.btnBell.setOnClickListener {
            val f = NotificationFragment().apply {
                arguments = Bundle().apply {
                    putString("homeId", homeId)
                    putLong("selectedDate", selectedDate.toEpochDay())
                }
            }
            replaceFragment2(parentFragmentManager, f, null)
        }

        // 룸 목록
        binding.rvRooms.layoutManager = GridLayoutManager(requireContext(), 3)
        roomsAdapter = RoomsAdapter(onClick = { room ->
            if (selectedRoomId == room.id) return@RoomsAdapter
            selectedRoomId = room.id
            val token = AppController.prefs.getToken().orEmpty()
            if (token.isNotBlank()) {
                viewModel.fetchEntryPatternsForSelection(token, homeId, selectedRoomId, selectedDate)
            }
            val display = listOf(Room(id = "ALL", name = "전체")) + viewModel.rooms.value
            roomsAdapter.submit(display, emptyMap(), selectedRoomId, false)
        })
        binding.rvRooms.adapter = roomsAdapter

        // 초기 데이터 로드
        val token = AppController.prefs.getToken().orEmpty()
        if (token.isNotBlank() && homeId.isNotBlank()) {
            viewModel.fetchRooms(token, homeId)
        }

        // 상태 수집
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 방 목록
                launch {
                    viewModel.rooms.collect { rooms ->
                        val display = listOf(Room(id = "ALL", name = "전체")) + rooms
                        binding.rvRooms.visibility = if (display.size > 1) View.VISIBLE else View.GONE
                        binding.tvRoomsTitle.visibility = if (display.size > 1) View.VISIBLE else View.GONE

                        // 어댑터 갱신
                        roomsAdapter.submit(display, emptyMap(), selectedRoomId, false)

                        // 차트 초기 로딩
                        val tk = AppController.prefs.getToken().orEmpty()
                        if (tk.isNotBlank()) {
                            viewModel.fetchEntryPatternsForSelection(tk, homeId, selectedRoomId, selectedDate)
                        }
                    }
                }

                // 차트 데이터 수집
                launch {
                    viewModel.chart.collect { points ->
                        binding.chart.setChart(
                            points,
                            selectedIdx = -1,
                            maxYOverride = viewModel.maxY()
                        )
                        val totalEnter = points.sumOf { it.enterCount }
                        val totalExit  = points.sumOf { it.exitCount }
                        binding.tvTotalEnter.text = totalEnter.toString()
                        binding.tvTotalExit.text  = totalExit.toString()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
