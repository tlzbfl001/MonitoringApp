package com.aitronbiz.aitron.view.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aitronbiz.aitron.R
import com.aitronbiz.aitron.databinding.FragmentDeviceEnrollResultBinding
import com.aitronbiz.aitron.entity.EnumData
import com.aitronbiz.aitron.util.CustomUtil.replaceFragment1

class DeviceEnrollResultFragment : Fragment() {
    private var _binding: FragmentDeviceEnrollResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceEnrollResultBinding.inflate(inflater, container, false)

        val resultTypeName = arguments?.getString("resultType") ?: "UNKNOWN"
        val resultType = EnumData.valueOf(resultTypeName)

        when(resultType) {
            EnumData.DONE -> showSuccessUI()
            EnumData.NO_SUBJECT -> showNoSubjectUI()
            EnumData.INVALID_NUMBER -> showInvalidNumberUI()
            EnumData.UNKNOWN -> showUnknownErrorUI()
            else -> showUnknownErrorUI()
        }

        binding.btnClose.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
        }

        return binding.root
    }

    private fun showSuccessUI() {
        binding.icon.setImageResource(R.drawable.ic_launcher_foreground)
        binding.title.text = "기기 등록 성공"
        binding.message.text = "기기 등록을 완료하였습니다"
        binding.button1.text = "확인"
        binding.button1.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, AddDeviceFragment())
        }
        binding.button2.visibility = View.GONE
    }

    private fun showNoSubjectUI() {
        binding.icon.setImageResource(R.drawable.ic_launcher_foreground)
        binding.title.text = "기기 등록 실패"
        binding.message.text = "보호 대상자가 없습니다\n등록 후 사용하세요"
        binding.button1.text = "보호 대상자 등록하기"
        binding.button1.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
        }
        binding.button2.visibility = View.GONE
    }

    private fun showInvalidNumberUI() {
        binding.icon.setImageResource(R.drawable.ic_launcher_foreground)
        binding.title.text = "기기 등록 실패"
        binding.message.text = "올바른 QR 코드가 아닙니다\n다시 시도해주세요"
        binding.button1.text = "제품 입력"
        binding.button1.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, AddDeviceFragment())
        }
        binding.button2.apply {
            visibility = View.VISIBLE
            text = "QR 스캔"
            setOnClickListener {
                replaceFragment1(requireActivity().supportFragmentManager, QrScanFragment())
            }
        }
    }

    private fun showUnknownErrorUI() {
        binding.icon.setImageResource(R.drawable.ic_launcher_foreground)
        binding.title.text = "기기 등록 실패"
        binding.message.text = "다시 시도해 주세요"
        binding.button1.text = "확인"
        binding.button1.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
        }
        binding.button2.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}