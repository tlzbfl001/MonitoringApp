package kr.aitron.aitron.view.home

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toDrawable
import kr.aitron.aitron.R
import kr.aitron.aitron.database.DataManager
import kr.aitron.aitron.databinding.FragmentAddDeviceBinding
import kr.aitron.aitron.entity.Device
import kr.aitron.aitron.util.CustomUtil.replaceFragment1
import java.time.LocalDateTime

class AddDeviceFragment : Fragment() {
    private var _binding: FragmentAddDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var dialog : Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDeviceBinding.inflate(inflater, container, false)

        dataManager = DataManager(requireActivity())
        dataManager.open()

        dialog = Dialog(requireActivity())
        dialog!!.setContentView(R.layout.dialog_warning)
        dialog!!.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val btnConfirm = dialog!!.findViewById<CardView>(R.id.btnConfirm)

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
        }

        binding.btnAdd.setOnClickListener {
            val getSubject = dataManager.getSubject(1)
            if(getSubject.createdAt != "") {
                if(binding.etProduct.text.trim().isEmpty()) {
                    Toast.makeText(requireActivity(), "제품 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                }else if (binding.etSerial.text.trim().isEmpty()) {
                    Toast.makeText(requireActivity(), "시리얼 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                }else {
                    val device = Device(
                        uid = 1,
                        name = "",
                        subjectId = getSubject.id,
                        productNumber = binding.etProduct.text.trim().toString(),
                        serialNumber = binding.etSerial.text.trim().toString(),
                        createdAt = LocalDateTime.now().toString(),
                    )

                    val success = dataManager.insertDevice(device)
                    if(success) {
                        Toast.makeText(requireActivity(), "등록되었습니다", Toast.LENGTH_SHORT).show()
                        replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
                    }else {
                        Toast.makeText(requireActivity(), "등록 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }else {
                dialog!!.show()
                btnConfirm.setOnClickListener {
                    replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
                    dialog!!.dismiss()
                }
            }
        }

        return binding.root
    }
}