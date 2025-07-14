package com.aitronbiz.arron.view.room

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentDetailRoomBinding
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.view.device.DeviceFragment
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.subject.SubjectFragment

class DetailRoomFragment : Fragment() {
    private var _binding: FragmentDetailRoomBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var homeData: Home? = null
    private var subjectData: Subject? = null
    private var roomData: Room? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailRoomBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())

        arguments?.let {
            homeData = it.getParcelable("homeData")
            subjectData = it.getParcelable("subjectData")
            roomData = it.getParcelable("roomData")
        }

        val args = Bundle().apply {
            putParcelable("homeData", homeData)
            putParcelable("subjectData", subjectData)
            putParcelable("roomData", roomData)
        }

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, RoomFragment(), args)
        }

        binding.btnAddDevice.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, DeviceFragment(), args)
        }

        return binding.root
    }
}