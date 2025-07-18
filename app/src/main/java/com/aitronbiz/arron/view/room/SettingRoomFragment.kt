package com.aitronbiz.arron.view.room

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.adapter.DeviceItemAdapter
import com.aitronbiz.arron.adapter.RoomItemAdapter
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentSettingRoomBinding
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.device.AddDeviceFragment
import com.aitronbiz.arron.view.home.SettingHomeFragment

class SettingRoomFragment : Fragment() {
    private var _binding: FragmentSettingRoomBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var adapter: DeviceItemAdapter
    private lateinit var deviceList: MutableList<Device>
    private var home: Home? = null
    private var room: Room? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingRoomBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())

        arguments?.let {
            home = it.getParcelable("home")
            room = it.getParcelable("room")
        }

        val args = Bundle().apply {
            putParcelable("home", home)
            putParcelable("room", room)
        }

        if(home != null) {
            binding.tvTitle.text = home!!.name
            deviceList = dataManager.getDevices(home!!.id, room!!.id).toMutableList()
        }else {
            deviceList = mutableListOf()
        }

        adapter = DeviceItemAdapter(
            deviceList,
            onItemClick = { device ->
                val bundle = Bundle().apply {
                    putParcelable("home", home)
                    putParcelable("room", room)
                    putParcelable("device", device)
                }
                replaceFragment2(parentFragmentManager, SettingRoomFragment(), bundle)
            }
        )

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, SettingHomeFragment(), args)
        }

        binding.btnAddDevice.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, AddDeviceFragment(), args)
        }

        return binding.root
    }
}