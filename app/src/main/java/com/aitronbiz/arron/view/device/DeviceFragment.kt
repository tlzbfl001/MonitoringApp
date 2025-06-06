package com.aitronbiz.arron.view.device

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.DeviceListAdapter
import com.aitronbiz.arron.adapter.SubjectDialogAdapter
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentDeviceBinding
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.home.AddSubjectFragment
import com.aitronbiz.arron.view.home.MainFragment

class DeviceFragment : Fragment() {
    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var adapter: DeviceListAdapter
    private var warningDialog : Dialog? = null
    private var optionalDialog : BottomSheetDialog? = null
    private var subjectDialog : BottomSheetDialog? = null
    private var subjectId = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        dataManager = DataManager.getInstance(requireContext())

        warningDialog = Dialog(requireActivity())
        warningDialog!!.setContentView(R.layout.dialog_warning)
        warningDialog!!.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val btnConfirm = warningDialog!!.findViewById<CardView>(R.id.btnConfirm)

        optionalDialog = BottomSheetDialog(requireContext())
        val optionalDialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
        optionalDialog!!.setContentView(optionalDialogView)

        subjectDialog = BottomSheetDialog(requireContext())
        val subjectDialogView = layoutInflater.inflate(R.layout.dialog_select_subject, null)
        val recyclerView = subjectDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddSubject = subjectDialogView.findViewById<ConstraintLayout>(R.id.btnAddSubject)

        val subjects = dataManager.getSubjects(AppController.prefs.getUID())
        if(subjects.isNotEmpty()) subjectId = subjects[0].id

        subjectDialog!!.setContentView(subjectDialogView)

        val selectSubjectDialogAdapter = SubjectDialogAdapter(subjects) { selectedItem ->
            subjectId = selectedItem.id
            binding.tvSubject.text = "대상자 : ${selectedItem.name}"
            Handler(Looper.getMainLooper()).postDelayed({
                val devices = dataManager.getDevices(subjectId) // Device 객체 리스트 반환
                adapter.updateData(devices)
                subjectDialog?.dismiss()
            }, 300)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = selectSubjectDialogAdapter

        btnAddSubject.setOnClickListener {
            subjectDialog?.dismiss()
            replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
        }

        adapter = DeviceListAdapter(mutableListOf(),
            onAddClick = {
                val btnOption1 = optionalDialogView.findViewById<CardView>(R.id.buttonOption1)
                val btnOption2 = optionalDialogView.findViewById<CardView>(R.id.buttonOption2)

                btnOption1.setOnClickListener {
                    if(subjects.isNotEmpty()) {
                        val bundle = Bundle()
                        bundle.putInt("subjectId", subjectId)
                        replaceFragment2(requireActivity().supportFragmentManager, AddDeviceFragment(), bundle)
                    }else {
                        warningDialog!!.show()
                        btnConfirm.setOnClickListener {
                            replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
                            warningDialog!!.dismiss()
                        }
                    }

                    optionalDialog!!.dismiss()
                }

                btnOption2.setOnClickListener {
                    if(subjects.isNotEmpty()) {
                        replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
                    }else {
                        warningDialog!!.show()
                        btnConfirm.setOnClickListener {
                            replaceFragment1(requireActivity().supportFragmentManager, QrScanFragment())
                            warningDialog!!.dismiss()
                        }
                    }

                    optionalDialog!!.dismiss()
                }

                optionalDialog!!.show()
            }
        )

        binding.recyclerDevices.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerDevices.adapter = adapter

        if(subjectId > 0) {
            val devices = dataManager.getDevices(subjectId)
            adapter.updateData(devices)
            binding.tvSubject.text = "대상자 : ${subjects[0].name}"
        }else {
            binding.tvSubject.text = "대상자 :   "
        }

        binding.btnSubject.setOnClickListener {
            subjectDialog!!.show()
        }

//        binding.deviceList.setOnClickListener {
//            bundle.putParcelable("device", Device(uid = 1, subjectId = 1, name = "test", productNumber = "11", serialNumber = "11", createdAt = ""))
//            replaceFragment2(requireActivity().supportFragmentManager, DeviceSettingFragment(), bundle)
//        }

        return binding.root
    }
}