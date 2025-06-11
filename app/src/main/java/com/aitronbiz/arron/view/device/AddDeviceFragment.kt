package com.aitronbiz.arron.view.device

import com.aitronbiz.arron.R
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentAddDeviceBinding
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import java.time.LocalDateTime
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//class AddDeviceFragment : Fragment(), OnMapReadyCallback {
class AddDeviceFragment : Fragment() {
    private var _binding: FragmentAddDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
//    private lateinit var map: GoogleMap
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var subjectId = 0
    private var room = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDeviceBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        dataManager = DataManager.getInstance(requireContext())

        arguments?.let {
            subjectId = it.getInt("subjectId", 0)
        }

        setupUI()
//        setupMap()

        return binding.root
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
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
                    uid = AppController.prefs.getUID(),
                    subjectId = subjectId,
                    name = binding.etName.text.trim().toString(),
                    productNumber = binding.etProduct.text.trim().toString(),
                    serialNumber = binding.etSerial.text.trim().toString(),
                    room = room,
                    createdAt = LocalDateTime.now().toString(),
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    val success = dataManager.insertDevice(device)
                    withContext(Dispatchers.Main) {
                        if(success) {
                            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
                            Toast.makeText(requireActivity(), "등록되었습니다", Toast.LENGTH_SHORT).show()
                        }else {
                            Toast.makeText(requireActivity(), "등록 실패하였습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    /*private fun setupMap() {
        // 지도 준비
        val mapFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.btnGetLocation.setOnClickListener {
            getCurrentLocation()
        }
    }
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isMyLocationButtonEnabled = false
        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001)
            return
        }
        map.isMyLocationEnabled = true
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)

                // 지도에 마커 표시
                map.clear()
                map.addMarker(MarkerOptions().position(latLng).title("현재 위치"))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))

                // 위치 정보 사용 가능
                Log.d("Maps", "현재 위치: ${it.latitude}, ${it.longitude}")
            } ?: run {
                Toast.makeText(requireActivity(), "위치를 가져올 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }*/
}