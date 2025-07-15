package com.aitronbiz.arron.view.room

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.UpdateRoomDTO
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentEditRoomBinding
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class EditRoomFragment : Fragment() {
    private var _binding: FragmentEditRoomBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var homeData: Home? = null
    private var subjectData: Subject? = null
    private var roomData: Room? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditRoomBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())

        homeData = arguments?.getParcelable("homeData")
        subjectData = arguments?.getParcelable("subjectData")
        roomData = arguments?.getParcelable("roomData")

        val bundle = Bundle().apply {
            putParcelable("homeData", homeData)
            putParcelable("subjectData", subjectData)
        }

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, RoomFragment(), bundle)
        }

        binding.btnEdit.setOnClickListener {
            val data = Room(
                id = roomData!!.id,
                uid = AppController.prefs.getUID(),
                name = binding.etName.text.trim().toString(),
                createdAt = LocalDateTime.now().toString(),
            )

            lifecycleScope.launch(Dispatchers.IO) {
                val updatedRows = dataManager.updateRoom(data)
                withContext(Dispatchers.Main) {
                    if(updatedRows > 0 && roomData?.serverId != null && roomData?.serverId != "") {
                        val dto = UpdateRoomDTO(name = binding.etName.text.trim().toString())
                        val response = RetrofitClient.apiService.updateRoom("Bearer ${AppController.prefs.getToken()}", roomData!!.serverId!!, dto)
                        if(response.isSuccessful) {
                            Log.d(TAG, "updateRoom: ${response.body()}")
                        } else {
                            Log.e(TAG, "updateRoom: $response")
                        }

                        Toast.makeText(requireActivity(), "수정되었습니다", Toast.LENGTH_SHORT).show()
                        replaceFragment2(requireActivity().supportFragmentManager, RoomFragment(), bundle)
                    }else {
                        Toast.makeText(requireActivity(), "수정 실패하였습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return binding.root
    }
}