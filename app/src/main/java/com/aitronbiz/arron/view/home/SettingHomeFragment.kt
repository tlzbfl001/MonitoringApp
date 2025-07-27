package com.aitronbiz.arron.view.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.adapter.RoomItemAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentSettingHomeBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.room.AddRoomFragment
import com.aitronbiz.arron.view.room.SettingRoomFragment
import com.google.gson.Gson
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
                    Toast.makeText(context, "홈 또는 룸 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
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
                        Log.e(TAG, "getAllRoom 실패: ${getAllRoom.code()}")
                    }
                } else {
                    Log.e(TAG, "getHome 실패: ${response.code()}")
                }
            }
        }

        binding.btnBack.setOnClickListener {
            replaceFragment1(parentFragmentManager, HomeFragment())
        }

        binding.btnAddRoom.setOnClickListener {
            if(homeId != "") {
                val bundle = Bundle().apply {
                    putString("homeId", homeId)
                }
                replaceFragment2(parentFragmentManager, AddRoomFragment(), bundle)
            }else {
                Toast.makeText(context, "홈 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.hideBottomNav()
    }
}