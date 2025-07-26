package com.aitronbiz.arron.view.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.viewmodel.MainViewModel
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.SelectHomeDialogAdapter
import com.aitronbiz.arron.adapter.WeekAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentMainBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.location
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.util.OnStartDragListener
import com.aitronbiz.arron.view.device.DeviceFragment
import com.aitronbiz.arron.view.notification.NotificationFragment
import com.aitronbiz.arron.view.setting.SettingsFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MainFragment : Fragment(), OnStartDragListener, CalendarPopupDialog.OnHomeSelectedListener {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var viewModel: MainViewModel
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var homeDialog: BottomSheetDialog? = null

    private var homes = ArrayList<Home>()
    private var homeId = ""
    private val today = LocalDate.now()
    private var selectedDate = today

    private val basePageIndex = 1000
    private val baseDate = today
    private val currentPage = basePageIndex

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        initUI()
        setupHomeDialog()
        observeViewModel()
        return binding.root
    }

    private fun initUI() {
        setStatusBar(requireActivity(), binding.mainLayout)
        location = 1

        dataManager = DataManager.getInstance(requireActivity())
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        // 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // 주간 달력 어댑터 설정
        binding.viewPager.adapter = WeekAdapter(
            context = requireContext(),
            homeId = homeId,
            baseDate = baseDate,
            selectedDate = selectedDate,
            onDateSelected = { date ->
                if (selectedDate != date) {
                    selectedDate = date
                    viewModel.updateSelectedDate(date)
                }
            }
        )

        binding.viewPager.post {
            binding.viewPager.setCurrentItem(currentPage, false)
        }

        binding.btnExpand.setOnClickListener {
            val dialog = CalendarPopupDialog.newInstance(homeId)
            dialog.setOnHomeSelectedListener(this)
            dialog.show(parentFragmentManager, "calendarDialog")
        }

        binding.btnHome.setOnClickListener { homeDialog?.show() }
        binding.btnAlarm.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, NotificationFragment())
        }
        binding.btnSetting.setOnClickListener { showPopupMenu(it) }
        binding.btnActivityDetection.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, ActivityDetectionFragment(), Bundle().apply {
                putString("homeId", homeId)
            })
        }
        binding.btnRespirationDetection.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, RespirationDetectionFragment(), Bundle().apply {
                putString("homeId", homeId)
            })
        }

        binding.tvHome.text = "나의 홈"
    }

    private fun observeViewModel() {
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            selectedDate = date

            // 선택 날짜 업데이트
            val adapter = binding.viewPager.adapter as? WeekAdapter
            adapter?.updateSelectedDate(date)

            val targetWeekStart = date.with(DayOfWeek.SUNDAY)
            val currentItemDate = baseDate.plusWeeks((binding.viewPager.currentItem - basePageIndex - 1).toLong()).with(DayOfWeek.SUNDAY)

            if (targetWeekStart != currentItemDate) {
                val weekDiff = ChronoUnit.WEEKS.between(baseDate.with(DayOfWeek.SUNDAY), targetWeekStart)
                val targetPage = basePageIndex + weekDiff.toInt()
                binding.viewPager.setCurrentItem(targetPage, false)
            }
        }
    }

    private fun scrollToWeek(date: LocalDate) {
        val weekDiff = ChronoUnit.WEEKS.between(baseDate.with(DayOfWeek.SUNDAY), date.with(DayOfWeek.SUNDAY))
        val position = basePageIndex + weekDiff.toInt()
        binding.viewPager.setCurrentItem(position, false)
    }

    override fun onHomeSelected(homeId: String, homeName: String) {
        this.homeId = homeId
        binding.tvHome.text = homeName
        scrollToWeek(viewModel.selectedDate.value ?: LocalDate.now())
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.main_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.device -> {
                    replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
                    true
                }
                R.id.setting -> {
                    replaceFragment1(requireActivity().supportFragmentManager, SettingsFragment())
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun setupHomeDialog() {
        homeDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_select_home, null)
        homeDialog!!.setContentView(view)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAdd = view.findViewById<ConstraintLayout>(R.id.btnAdd)

        btnAdd.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, HomeFragment())
            homeDialog?.dismiss()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val response = RetrofitClient.apiService.getAllHome("Bearer ${AppController.prefs.getToken()}")
            if (response.isSuccessful) {
                homes = response.body()?.homes ?: arrayListOf()

                withContext(Dispatchers.Main) {
                    // 현재 선택된 homeId가 homes 리스트에 없을 수 있으므로 indexOfFirst로 찾고 없으면 0으로 설정
                    val selectedIndex = homes.indexOfFirst { it.id == homeId }.takeIf { it != -1 } ?: 0

                    // 실제 homeId 값을 업데이트
                    if (homes.isNotEmpty()) {
                        homeId = homes[selectedIndex].id
                        binding.tvHome.text = homes[selectedIndex].name
                    } else {
                        binding.tvHome.text = "홈"
                    }

                    val adapter = SelectHomeDialogAdapter(
                        items = homes,
                        selectedPosition = selectedIndex,
                        onItemClick = { selectedHome ->
                            homeId = selectedHome.id
                            binding.tvHome.text = selectedHome.name
                            Handler(Looper.getMainLooper()).postDelayed({ homeDialog?.dismiss() }, 300)
                        }
                    )

                    recyclerView.layoutManager = LinearLayoutManager(requireContext())
                    recyclerView.adapter = adapter
                }
            } else {
                // 실패한 경우 로깅 또는 예외 처리
                val errorBody = response.errorBody()?.string()
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                Log.e("MainFragment", "getAllHome: $errorResponse")
            }
        }
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.showBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
