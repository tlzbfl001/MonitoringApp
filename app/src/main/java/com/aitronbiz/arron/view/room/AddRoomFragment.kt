package com.aitronbiz.arron.view.room

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentAddRoomBinding
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.home.MainFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class AddRoomFragment : Fragment() {
    private var _binding: FragmentAddRoomBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var homeId = 0
    private var status = EnumData.NORMAL.name

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddRoomBinding.inflate(inflater, container, false)

        setupUI()

        return binding.root
    }

    private fun setupUI() {
        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireContext())

        arguments?.let {
            homeId = it.getInt("homeId", 0)
        }

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, RoomFragment())
        }

        binding.btnNormal.setOnClickListener {
            status = EnumData.NORMAL.name
            updateStatusButtonUI(binding.btnNormal)
        }

        binding.btnCaution.setOnClickListener {
            status = EnumData.CAUTION.name
            updateStatusButtonUI(binding.btnCaution)
        }

        binding.btnWarning.setOnClickListener {
            status = EnumData.WARNING.name
            updateStatusButtonUI(binding.btnWarning)
        }

        binding.btnAdd.setOnClickListener {
            val name = binding.etName.text.trim().toString()

            when {
                name.isEmpty() -> Toast.makeText(requireActivity(), "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                homeId == 0 -> Toast.makeText(requireActivity(), "등록된 홈이 없습니다. 홈 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                else -> {
                    val room = Room(
                        uid = AppController.prefs.getUID(),
                        homeId = homeId,
                        name = name,
                        status = status,
                        createdAt = LocalDateTime.now().toString()
                    )

                    lifecycleScope.launch(Dispatchers.IO) {
                        val success = dataManager.insertRoom(room)
                        withContext(Dispatchers.Main) {
                            if(success) {
                                Toast.makeText(requireActivity(), "저장되었습니다", Toast.LENGTH_SHORT).show()
                                replaceFragment1(requireActivity().supportFragmentManager, RoomFragment())
                            }else {
                                Toast.makeText(requireActivity(), "저장 실패하였습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateStatusButtonUI(selected: View) {
        val buttons = listOf(binding.btnNormal, binding.btnCaution, binding.btnWarning)
        for (btn in buttons) {
            val drawableId = if (btn == selected) R.drawable.rec_30_black else R.drawable.rec_30_grey
            btn.background = ContextCompat.getDrawable(requireContext(), drawableId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}