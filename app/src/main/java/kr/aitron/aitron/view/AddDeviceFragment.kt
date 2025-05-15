package kr.aitron.aitron.view

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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kr.aitron.aitron.MainViewModel
import kr.aitron.aitron.R
import kr.aitron.aitron.databinding.FragmentAddDeviceBinding
import kr.aitron.aitron.database.entity.Device
import kr.aitron.aitron.util.CustomUtil.replaceFragment1
import java.time.LocalDateTime

class AddDeviceFragment : Fragment() {
    private var _binding: FragmentAddDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private var dialog : Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDeviceBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        dialog = Dialog(requireActivity())
        dialog!!.setContentView(R.layout.dialog_warning)
        dialog!!.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val btnConfirm = dialog!!.findViewById<CardView>(R.id.btnConfirm)

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
        }

        binding.btnAdd.setOnClickListener {
            viewModel.subjectLiveData.observe(viewLifecycleOwner, Observer { subject ->
                if (subject != null && subject.id > 0) {
                    if(binding.etProduct.text.trim().isEmpty()) {
                        Toast.makeText(requireActivity(), "제품 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                    }else if (binding.etSerial.text.trim().isEmpty()) {
                        Toast.makeText(requireActivity(), "시리얼 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                    }else {
                        val device = Device(
                            uid = 1,
                            name = "",
                            subjectId = subject.id,
                            productNumber = binding.etProduct.text.trim().toString(),
                            serialNumber = binding.etSerial.text.trim().toString(),
                            createdAt = LocalDateTime.now().toString(),
                        )

                        viewModel.insertDevice(device)
                    }
                } else {
                    dialog!!.show()
                    btnConfirm.setOnClickListener {
                        replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
                        dialog!!.dismiss()
                    }
                }
            })
        }

        viewModel.deviceInsertedLiveData.observe(viewLifecycleOwner, Observer { isInserted ->
            if (isInserted) {
                Toast.makeText(requireActivity(), "등록되었습니다", Toast.LENGTH_SHORT).show()
                viewModel.resetInsertState() // 데이터 삽입 후 상태를 리셋
                replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
            }
        })

        return binding.root
    }
}