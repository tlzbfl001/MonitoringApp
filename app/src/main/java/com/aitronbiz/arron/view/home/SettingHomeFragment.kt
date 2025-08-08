package com.aitronbiz.arron.view.home

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.RoomItemAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.databinding.FragmentSettingHomeBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.room.AddRoomFragment
import com.aitronbiz.arron.view.room.EditRoomFragment
import com.aitronbiz.arron.view.room.SettingRoomFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingHomeFragment : Fragment() {
    private var _binding: FragmentSettingHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RoomItemAdapter
    private var roomList: MutableList<Room> = mutableListOf()
    private var homeId: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingHomeBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        homeId = arguments?.getString("homeId")

        adapter = RoomItemAdapter(
            roomList,
            onItemClick = { room ->
                if(homeId != "" && room.id != "") {
                    val bundle = Bundle().apply {
                        putString("homeId", homeId)
                        putString("roomId", room.id)
                    }
                    replaceFragment2(parentFragmentManager, SettingRoomFragment(), bundle)
                }else {
                    Toast.makeText(context, "홈 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            },
            onAddClick = {
                if(homeId != "") {
                    val bundle = Bundle().apply {
                        putString("homeId", homeId)
                    }
                    replaceFragment2(parentFragmentManager, AddRoomFragment(), bundle)
                }else {
                    Toast.makeText(context, "홈 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch(Dispatchers.IO) {
            if(homeId != null) {
                val response = RetrofitClient.apiService.getHome("Bearer ${AppController.prefs.getToken()}", homeId!!)
                if(response.isSuccessful) {
                    val homeName = response.body()!!.home.name
                    val getAllRoom = RetrofitClient.apiService.getAllRoom("Bearer ${AppController.prefs.getToken()}", homeId!!)
                    if(getAllRoom.isSuccessful) {
                        val fetchedRooms = getAllRoom.body()!!.rooms
                        withContext(Dispatchers.Main) {
                            binding.tvTitle.text = homeName
                            roomList.clear()
                            roomList.addAll(fetchedRooms)
                            adapter.notifyDataSetChanged()
                        }
                    } else {
                        Log.e(TAG, "getAllRoom: ${getAllRoom.code()}")
                    }
                } else {
                    Log.e(TAG, "getAllRoom: ${response.code()}")
                }
            }
        }

        binding.btnBack.setOnClickListener {
            replaceFragment1(parentFragmentManager, HomeFragment())
        }

        binding.btnSetting.setOnClickListener { view ->
            showCustomPopupWindow(view)
        }

        return binding.root
    }

    private fun showCustomPopupWindow(anchor: View) {
        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.popup_delete_layout, null)

        val popupWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 180f, anchor.resources.displayMetrics
        ).toInt()

        val screenWidth = resources.displayMetrics.widthPixels
        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val anchorX = anchorLocation[0]

        // anchor 기준 팝업이 화면을 넘지 않도록 왼쪽으로 offset 계산
        val offsetX = if (anchorX + popupWidth > screenWidth) {
            screenWidth - (anchorX + popupWidth) - 20 // -20은 추가 margin
        } else {
            -20 // 기본 왼쪽 offset
        }

        val popupWindow = PopupWindow(popupView, popupWidth, WindowManager.LayoutParams.WRAP_CONTENT, true)
        popupWindow.elevation = 10f
        popupWindow.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        popupWindow.isOutsideTouchable = true

        popupWindow.showAsDropDown(anchor, offsetX, 0)

        popupView.findViewById<TextView>(R.id.menuEdit).setOnClickListener {
            replaceFragment1(parentFragmentManager, EditHomeFragment())
        }

        popupView.findViewById<TextView>(R.id.menuDelete).setOnClickListener {
            val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
                .setTitle("홈 삭제")
                .setMessage("정말 삭제 하시겠습니까?")
                .setPositiveButton("확인", null)
                .setNegativeButton("취소", null)
                .create()

            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val response = RetrofitClient.apiService.deleteHome("Bearer ${AppController.prefs.getToken()}", homeId!!)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Log.d(TAG, "deleteHome: ${response.body()}")
                                Toast.makeText(requireContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                replaceFragment1(requireActivity().supportFragmentManager, HomeFragment())
                                dialog.dismiss()
                            } else {
                                Log.e(TAG, "deleteHome: ${response.code()}")
                                Toast.makeText(requireContext(), "삭제 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            dialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.hideBottomNav()
    }
}