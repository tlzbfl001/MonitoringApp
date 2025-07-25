package com.aitronbiz.arron.view.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentMainBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.util.OnStartDragListener
import com.aitronbiz.arron.view.notification.NotificationFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MainFragment : Fragment(), OnStartDragListener {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var viewModel: MainViewModel
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var homeDialog: BottomSheetDialog? = null
    private var homeId = ""
    private val today = LocalDate.now()
    private var selectedDate = today
    private val baseWeekStart = today.with(DayOfWeek.SUNDAY)
    private val basePageIndex = 1000
    private val currentWeekStart = today.with(DayOfWeek.SUNDAY)
    private val weekOffset = baseWeekStart.until(currentWeekStart).days / 7
    private val currentPage = basePageIndex + weekOffset
    private var isFirstObserve = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        initUI()
        setupHomeDialog()

        return binding.root
    }

    private fun initUI() {
        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        viewModel.updateSelectedDate(LocalDate.now())
        isFirstObserve = true

        val user = dataManager.getUser(AppController.prefs.getUID())
        Log.d(TAG, "sessionToken: ${user.sessionToken}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        binding.viewPager.adapter = WeekAdapter(
            requireContext(),
            homeId = homeId,
            baseDate = baseWeekStart,
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                viewModel.updateSelectedDate(date)
            }
        )

        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            selectedDate = date

            val weekAdapter = binding.viewPager.adapter as? WeekAdapter
            weekAdapter?.updateSelectedDate(date)

            if (isFirstObserve) {
                isFirstObserve = false
                return@observe
            }

            val sunday = date.with(DayOfWeek.SUNDAY)
            val weekOffset = ChronoUnit.WEEKS.between(baseWeekStart, sunday).toInt()
            val targetPage = basePageIndex + weekOffset
            binding.viewPager.setCurrentItem(targetPage, true)
        }

        binding.viewPager.post {
            binding.viewPager.setCurrentItem(currentPage, false)
        }

        binding.btnAlarm.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, NotificationFragment())
        }

        binding.btnSetting.setOnClickListener {

        }

        binding.btnExpand.setOnClickListener {
            val dialog = CalendarPopupDialog.newInstance(homeId)
            dialog.show(parentFragmentManager, "calendar_dialog")
        }

        binding.btnHome.setOnClickListener {
            homeDialog!!.show()
        }

        binding.btnActivityDetection.setOnClickListener {
            val bundle = Bundle().apply {
                putString("homeId", homeId)
            }
            replaceFragment2(requireActivity().supportFragmentManager, ActivityDetectionFragment(), bundle)
        }

        binding.btnRespirationDetection.setOnClickListener {
            val bundle = Bundle().apply {
                putString("homeId", homeId)
            }
            replaceFragment2(requireActivity().supportFragmentManager, RespirationDetectionFragment(), bundle)
        }
    }

    private fun setupHomeDialog() {
        homeDialog = BottomSheetDialog(requireContext())
        val homeDialogView = layoutInflater.inflate(R.layout.dialog_select_home, null)
        val homeRecyclerView = homeDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddHome = homeDialogView.findViewById<ConstraintLayout>(R.id.btnAdd)
        homeDialog!!.setContentView(homeDialogView)

        btnAddHome.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, HomeFragment())
            homeDialog?.dismiss()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val response = RetrofitClient.apiService.getAllHome("Bearer ${AppController.prefs.getToken()}")

            if (response.isSuccessful) {
                val homes = response.body()!!.homes

                withContext(Dispatchers.Main) {
                    val selectHomeDialogAdapter = SelectHomeDialogAdapter(homes) { selectedItem ->
                        homeId = selectedItem.id
                        binding.tvHome.text = selectedItem.name
                        Handler(Looper.getMainLooper()).postDelayed({
                            homeDialog?.dismiss()
                        }, 300)
                    }

                    homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    homeRecyclerView.adapter = selectHomeDialogAdapter

                    if (homes.isNotEmpty()) {
                        homeId = homes[0].id
                        binding.tvHome.text = homes[0].name
                    } else {
                        binding.tvHome.text = "홈"
                    }
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                Log.e(TAG, "getAllHome: $errorResponse")
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