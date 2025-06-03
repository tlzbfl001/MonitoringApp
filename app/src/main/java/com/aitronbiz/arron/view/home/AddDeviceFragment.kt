package com.aitronbiz.arron.view.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import android.widget.AdapterView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.adapter.SelectSubjectDialogAdapter
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import java.time.LocalDateTime
import com.aitronbiz.arron.databinding.FragmentAddDeviceBinding
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.google.android.material.bottomsheet.BottomSheetDialog

class AddDeviceFragment : Fragment() {
    private var _binding: FragmentAddDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var subjectDialog : BottomSheetDialog? = null
    private var subjectId = 0
    private var room = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDeviceBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        dataManager = DataManager(requireActivity())
        dataManager.open()

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
        }

        subjectDialog = BottomSheetDialog(requireContext())
        val subjectDialogView = layoutInflater.inflate(R.layout.dialog_select_subject, null)
        val recyclerView = subjectDialogView.findViewById<RecyclerView>(R.id.recyclerView)

        val subjects = dataManager.getSubjects(AppController.prefs.getUserPrefs())
        subjectId = subjects[0].id

        val selectSubjectDialogAdapter = SelectSubjectDialogAdapter(subjects) { selectedItem ->
            subjectId = selectedItem.id
            binding.tvSubject.text = "대상자 : ${selectedItem.name}"

            // 시간 여유주고 다이얼로그 닫기
            Handler(Looper.getMainLooper()).postDelayed({
                subjectDialog?.dismiss()
            }, 300)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = selectSubjectDialogAdapter

        subjectDialog!!.setContentView(subjectDialogView)

        binding.btnSubject.setOnClickListener {
            subjectDialog!!.show()
        }

        binding.btnAbsent.setOnClickListener {
            room = 0
            binding.btnAbsent.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_black))
            binding.btnPresent.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_grey))
        }

        binding.btnPresent.setOnClickListener {
            room = 1
            binding.btnAbsent.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_grey))
            binding.btnPresent.setBackgroundDrawable(resources.getDrawable(R.drawable.rec_30_black))
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