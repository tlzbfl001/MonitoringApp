package com.aitronbiz.arron.view.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.aitronbiz.arron.databinding.FragmentMainBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.location
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.safeApiCall
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

    private lateinit var viewModel: MainViewModel
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var homeDialog: BottomSheetDialog? = null

    private var homes = ArrayList<Home>()
    private var homeId = ""
    private val today = LocalDate.now()
    private var selectedDate = today

    private val basePageIndex = 1000
    private val baseDate = today
    private val currentPage = basePageIndex + 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        setStatusBar(requireActivity(), binding.mainLayout)
        location = 1

        // Android 13 이상에서 알림 권한 요청
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

        // 선택된 홈 ID가 변경되면 UI에 반영
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedHomeId.collect { id ->
                    if (id.isNotBlank()) {
                        homeId = id
                        val matched = homes.find { it.id == id }
                        if (matched != null) {
                            binding.tvHome.text = matched.name
                        }
                    }
                }
            }
        }

        // 날짜 변경 시 뷰페이저 위치 동기화
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            selectedDate = date
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

        setupHomeDialog()
        initUI()
        return binding.root
    }

    private fun initUI() {
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

        // 초기 위치 설정
        binding.viewPager.post {
            binding.viewPager.setCurrentItem(currentPage, false)
        }

        binding.btnExpand.setOnClickListener {
            if(homeId != "") {
                val dialog = CalendarPopupDialog.newInstance(homeId)
                dialog.setOnHomeSelectedListener(this)
                dialog.show(parentFragmentManager, "calendarDialog")
            }else {
                Toast.makeText(context, "홈 정보가 없어서 캘린더를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnHome.setOnClickListener { homeDialog?.show() }
        binding.btnAlarm.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, NotificationFragment())
        }
        binding.btnSetting.setOnClickListener { view ->
            showCustomPopupWindow(view)
        }
        binding.btnActivityDetection.setOnClickListener {
            if(homeId != "") {
                replaceFragment2(requireActivity().supportFragmentManager, ActivityDetectionFragment(), Bundle().apply {
                    putString("homeId", homeId)
                })
            }else {
                Toast.makeText(context, "홈 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnRespirationDetection.setOnClickListener {
            if(homeId != "") {
                replaceFragment2(requireActivity().supportFragmentManager, RespirationDetectionFragment(), Bundle().apply {
                    putString("homeId", homeId)
                })
            }else {
                Toast.makeText(context, "홈 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnFallDetection.setOnClickListener {
            if(homeId != "") {
                replaceFragment2(requireActivity().supportFragmentManager, FallDetectionFragment(), Bundle().apply {
                    putString("homeId", homeId)
                })
            }else {
                Toast.makeText(context, "홈 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnLifePattern.setOnClickListener {
            if(homeId != "") {
                replaceFragment2(requireActivity().supportFragmentManager, LifePatternsFragment(), Bundle().apply {
                    putString("homeId", homeId)
                })
            }else {
                Toast.makeText(context, "홈 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 홈 선택 시 호출되는 콜백
    override fun onHomeSelected(homeId: String, homeName: String) {
        this.homeId = homeId
        binding.tvHome.text = homeName
        val date = viewModel.selectedDate.value ?: LocalDate.now()
        val weekDiff = ChronoUnit.WEEKS.between(baseDate.with(DayOfWeek.SUNDAY), date.with(DayOfWeek.SUNDAY))
        val position = basePageIndex + weekDiff.toInt()
        binding.viewPager.setCurrentItem(position, false)
    }

    // 설정 버튼 클릭 시 팝업 메뉴 표시
    private fun showCustomPopupWindow(anchor: View) {
        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.popup_menu_layout, null)

        val popupWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 180f, anchor.resources.displayMetrics
        ).toInt()

        val screenWidth = resources.displayMetrics.widthPixels
        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val anchorX = anchorLocation[0]

        // anchor 기준 팝업이 화면을 넘지 않도록 왼쪽으로 offset 계산
        val offsetX = if (anchorX + popupWidth > screenWidth) {
            screenWidth - (anchorX + popupWidth) - 20
        } else {
            -20
        }

        val popupWindow = PopupWindow(popupView, popupWidth, WindowManager.LayoutParams.WRAP_CONTENT, true)
        popupWindow.elevation = 10f
        popupWindow.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        popupWindow.isOutsideTouchable = true

        popupWindow.showAsDropDown(anchor, offsetX, 0)

        popupView.findViewById<TextView>(R.id.menuDevice).setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
            popupWindow.dismiss()
        }

        popupView.findViewById<TextView>(R.id.menuSetting).setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, SettingsFragment())
            popupWindow.dismiss()
        }
    }

    // 홈 선택 다이얼로그 설정
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
            val response = safeApiCall {
                RetrofitClient.apiService.getAllHome("Bearer ${AppController.prefs.getToken()}")
            }

            if (response != null && response.isSuccessful) {
                homes = response.body()?.homes ?: arrayListOf()

                withContext(Dispatchers.Main) {
                    if (homes.isNotEmpty()) {
                        // 현재 선택된 homeId가 유효하지 않으면 첫 번째 홈을 기본으로 설정
                        val validIndex = homes.indexOfFirst { it.id == homeId }.takeIf { it != -1 } ?: 0

                        homeId = homes[validIndex].id
                        viewModel.setSelectedHomeId(homeId)
                        binding.tvHome.text = homes[validIndex].name
                    }

                    // 홈 리스트 어댑터 설정
                    val adapter = SelectHomeDialogAdapter(
                        items = homes,
                        selectedPosition = homes.indexOfFirst { it.id == homeId },
                        onItemClick = { selectedHome ->
                            homeId = selectedHome.id
                            viewModel.setSelectedHomeId(homeId)
                            binding.tvHome.text = selectedHome.name
                            Handler(Looper.getMainLooper()).postDelayed({
                                homeDialog?.dismiss()
                            }, 300)
                        }
                    )

                    recyclerView.layoutManager = LinearLayoutManager(requireContext())
                    recyclerView.adapter = adapter
                }
            } else {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "getAllHome 실패 또는 응답 없음")
                }
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
