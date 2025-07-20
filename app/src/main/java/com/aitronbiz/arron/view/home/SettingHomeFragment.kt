package com.aitronbiz.arron.view.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.adapter.RoomItemAdapter
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentSettingHomeBinding
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.room.AddRoomFragment
import com.aitronbiz.arron.view.room.SettingRoomFragment

class SettingHomeFragment : Fragment() {
    private var _binding: FragmentSettingHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var adapter: RoomItemAdapter
    private lateinit var roomList: MutableList<Room>
    private var home: Home? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingHomeBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())

        home = arguments?.getParcelable("home")

        if(home != null) {
            binding.tvTitle.text = home!!.name
            roomList = dataManager.getRooms(AppController.prefs.getUID(), home!!.id).toMutableList()
        }else {
            roomList = mutableListOf()
        }

        adapter = RoomItemAdapter(
            roomList,
            onItemClick = { room ->
                val bundle = Bundle().apply {
                    putParcelable("home", home)
                    putParcelable("room", room)
                }
                replaceFragment2(parentFragmentManager, SettingRoomFragment(), bundle)
            }
        )

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter

        binding.btnBack.setOnClickListener {
            replaceFragment1(parentFragmentManager, HomeFragment())
        }

        binding.btnAddRoom.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable("home", home)
            }
            replaceFragment2(parentFragmentManager, AddRoomFragment(), bundle)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.hideBottomNav()
    }
}