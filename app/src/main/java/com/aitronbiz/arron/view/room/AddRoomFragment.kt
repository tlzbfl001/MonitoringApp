package com.aitronbiz.arron.view.room

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.RoomDTO
import com.aitronbiz.arron.api.dto.SubjectDTO
import com.aitronbiz.arron.database.DBHelper.Companion.ROOM
import com.aitronbiz.arron.database.DBHelper.Companion.SUBJECT
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentAddRoomBinding
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.home.HomeFragment
import com.aitronbiz.arron.view.home.MainFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class AddRoomFragment : Fragment() {
    private var _binding: FragmentAddRoomBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var homeData: Home? = null
    private var subjectData: Subject? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddRoomBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireContext())

        arguments?.let {
            homeData = it.getParcelable("homeData")
            subjectData = it.getParcelable("subjectData")
        }

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, RoomFragment())
        }

        binding.btnAdd.setOnClickListener {
            val name = binding.etName.text.trim().toString()
            when {
                name.isEmpty() -> Toast.makeText(requireActivity(), "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                homeData == null -> Toast.makeText(requireActivity(), "등록된 홈이 없습니다. 홈 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                subjectData == null -> Toast.makeText(requireActivity(), "등록된 대상자가 없습니다. 홈 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                else -> {
                    val data = Room(
                        uid = AppController.prefs.getUID(),
                        homeId = homeData!!.id,
                        subjectId = subjectData!!.id,
                        name = name,
                        createdAt = LocalDateTime.now().toString()
                    )

                    lifecycleScope.launch(Dispatchers.IO) {
                        val insertedId = dataManager.insertRoom(data)
                        withContext(Dispatchers.Main) {
                            if(insertedId != -1) {
                                val dto = RoomDTO(
                                    name = data.name!!,
                                    homeId = homeData!!.serverId!!
                                )
                                val response = RetrofitClient.apiService.createRoom("Bearer ${AppController.prefs.getToken()}", dto)
                                if(response.isSuccessful) {
                                    Log.d(TAG, "createRoom: ${response.body()}")
                                    dataManager.updateData(ROOM, "serverId", response.body()!!.room.id, insertedId)
                                } else {
                                    Log.e(TAG, "createRoom: $response")
                                }

                                Toast.makeText(requireActivity(), "저장되었습니다", Toast.LENGTH_SHORT).show()

                                val args = Bundle().apply {
                                    putParcelable("homeData", homeData)
                                    putParcelable("subjectData", subjectData)
                                }
                                replaceFragment2(requireActivity().supportFragmentManager, RoomFragment(), args)
                            }else {
                                Toast.makeText(requireActivity(), "저장 실패하였습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}