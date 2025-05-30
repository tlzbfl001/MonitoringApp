package com.aitronbiz.arron.view.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.SubjectAdapter
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import java.time.LocalDateTime
import com.aitronbiz.arron.databinding.FragmentAddDeviceBinding
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2

class AddDeviceFragment : Fragment() {
    private var _binding: FragmentAddDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var subjectAdapter: SubjectAdapter
    private var status = EnumData.NORMAL.name
    private var subjectId = 0
    private var room = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDeviceBinding.inflate(inflater, container, false)

        dataManager = DataManager(requireActivity())
        dataManager.open()

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val subjects = dataManager.getSubjects(AppController.prefs.getUserPrefs())
        if(subjects.isNotEmpty()) {
            subjectAdapter = SubjectAdapter(subjects)
            binding.recyclerView.adapter = subjectAdapter
            subjectId = subjects[0].id

            subjectAdapter.setOnItemClickListener(object : SubjectAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    subjectId = subjects[position].id
                    subjectAdapter.setSelectedPosition(position)
                }
            })
        }

        binding.btnAbsent.setOnClickListener {
            room = 0
            binding.btnAbsent.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_purple))
            binding.btnPresent.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_grey))
        }

        binding.btnPresent.setOnClickListener {
            room = 1
            binding.btnAbsent.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_grey))
            binding.btnPresent.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_purple))
        }

        binding.btnNormal.setOnClickListener {
            status = EnumData.NORMAL.name
            binding.btnNormal.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_purple))
            binding.btnCaution.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_grey))
            binding.btnWarning.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_grey))
        }

        binding.btnCaution.setOnClickListener {
            status = EnumData.CAUTION.name
            binding.btnNormal.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_grey))
            binding.btnCaution.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_purple))
            binding.btnWarning.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_grey))
        }

        binding.btnWarning.setOnClickListener {
            status = EnumData.WARNING.name
            binding.btnNormal.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_grey))
            binding.btnCaution.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_grey))
            binding.btnWarning.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_purple))
        }

        binding.btnAdd.setOnClickListener {
            if(binding.etName.text.trim().isEmpty()) {
                Toast.makeText(requireActivity(), "장소 이름을 입력하세요", Toast.LENGTH_SHORT).show()
            }else if (binding.etProduct.text.trim().isEmpty()) {
                Toast.makeText(requireActivity(), "제품 번호를 입력하세요", Toast.LENGTH_SHORT).show()
            }else if (binding.etSerial.text.trim().isEmpty()) {
                Toast.makeText(requireActivity(), "시리얼 번호를 입력하세요", Toast.LENGTH_SHORT).show()
            }else {
                val device = Device(
                    uid = AppController.prefs.getUserPrefs(),
                    subjectId = subjectId,
                    name = binding.etName.text.trim().toString(),
                    productNumber = binding.etProduct.text.trim().toString(),
                    serialNumber = binding.etSerial.text.trim().toString(),
                    room = room,
                    status = status,
                    createdAt = LocalDateTime.now().toString(),
                )

                val success = dataManager.insertDevice(device)
                if(success) {
                    replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
                    Toast.makeText(requireActivity(), "등록되었습니다", Toast.LENGTH_SHORT).show()
                }else {
                    Toast.makeText(requireActivity(), "등록 실패하였습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return binding.root
    }
}