package com.aitronbiz.arron.view.room

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.adapter.RoomAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.database.DBHelper.Companion.ROOM
import com.aitronbiz.arron.database.DBHelper.Companion.SUBJECT
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentRoomBinding
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.home.MainFragment
import com.aitronbiz.arron.view.subject.DetailSubjectFragment
import com.aitronbiz.arron.view.subject.EditSubjectFragment
import com.aitronbiz.arron.view.subject.SubjectFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomFragment : Fragment() {
    private var _binding: FragmentRoomBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var adapter: RoomAdapter
    private var homeData: Home? = null
    private var subjectData: Subject? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())

        arguments?.let {
            homeData = it.getParcelable("homeData")
            subjectData = it.getParcelable("subjectData")
        }

        val list = if (homeData != null && subjectData != null) {
            dataManager.getRooms(AppController.prefs.getUID(), homeData!!.id)
        } else {
            mutableListOf()
        }

        adapter = RoomAdapter(
            list,
            onItemClick = { room ->
                if (homeData == null || subjectData == null) {
                    Toast.makeText(requireContext(), "홈과 대상자를 먼저 선택해주세요.", Toast.LENGTH_SHORT).show()
                    return@RoomAdapter
                }

                val bundle = Bundle().apply {
                    putParcelable("homeData", homeData)
                    putParcelable("subjectData", subjectData)
                    putParcelable("roomData", room)
                }
                replaceFragment2(parentFragmentManager, DetailRoomFragment(), bundle)
            },
            onEditClick = { room ->
                val bundle = Bundle().apply {
                    putParcelable("homeData", homeData)
                    putParcelable("subjectData", subjectData)
                    putParcelable("roomData", room)
                }
                replaceFragment2(parentFragmentManager, EditRoomFragment(), bundle)
            },
            onDeleteClick = { room ->
                lifecycleScope.launch(Dispatchers.IO) {
                    if(room.serverId != null && room.serverId != "") {
                        val response = RetrofitClient.apiService.deleteRoom("Bearer ${AppController.prefs.getToken()}", room.serverId!!)
                        if(response.isSuccessful) {
                            Log.d(TAG, "deleteRoom: ${response.body()}")
                        } else {
                            Log.e(TAG, "deleteRoom: $response")
                        }
                    }

                    dataManager.deleteData(ROOM, room.id)
                    list.removeIf { it.id == room.id }
                    withContext(Dispatchers.Main) {
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        )

        // RecyclerView 연결
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, SubjectFragment())
        }

        binding.btnAdd.setOnClickListener {
            if(homeData != null) {
                val args = Bundle().apply {
                    putParcelable("homeData", homeData)
                    putParcelable("subjectData", subjectData)
                }
                replaceFragment2(requireActivity().supportFragmentManager, AddRoomFragment(), args)
            }else {
                Toast.makeText(requireActivity(), "등록된 홈이 없습니다", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}