package com.aitronbiz.arron.view.setting

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitronbiz.arron.adapter.BluetoothDeviceAdapter
import com.aitronbiz.arron.databinding.FragmentConnectBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import androidx.core.view.isVisible
import com.aitronbiz.arron.database.DBHelper
import com.aitronbiz.arron.database.RealTimeDao
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import java.time.LocalDateTime

@SuppressLint("MissingPermission")
class ConnectFragment : Fragment() {
    private var _binding: FragmentConnectBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DBHelper
    private lateinit var dao: RealTimeDao
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private val receivedBuffer = StringBuilder()
    private var isConnected = false
    private var connectedDevice: BluetoothDevice? = null
    private lateinit var adapter: BluetoothDeviceAdapter

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    // 블루투스 연결/해제 이벤트 감지
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    adapter.updateConnectedDevice(device)
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (device?.address == adapter.getConnectedDevice()?.address) {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "${device?.name} 연결이 끊어졌습니다", Toast.LENGTH_SHORT).show()
                            handleDisconnection()
                            adapter.updateConnectedDevice(null)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConnectBinding.inflate(inflater, container, false)
        setStatusBar(requireActivity(), binding.mainLayout)

        dbHelper = DBHelper(requireContext())
        dao = RealTimeDao(dbHelper)

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, SettingsFragment())
        }

        binding.btnData.setOnClickListener {
            if (!isConnected) {
                Toast.makeText(requireContext(), "기기가 연결되어 있지 않습니다", Toast.LENGTH_SHORT).show()
            } else {
                binding.textScroll.visibility = View.VISIBLE
                readBluetoothData()
                binding.textView.text = receivedBuffer.toString()
                scrollToBottom()
            }
        }

        startBluetoothProcess()
        registerBluetoothReceiver()

        return binding.root
    }

    // 블루투스 연결 상태 감지 리시버 등록
    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        requireContext().registerReceiver(bluetoothReceiver, filter)
    }

    private fun unregisterBluetoothReceiver() {
        try {
            requireContext().unregisterReceiver(bluetoothReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver not registered or already unregistered")
        }
    }

    private fun startBluetoothProcess() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Toast.makeText(requireContext(), "블루투스가 꺼져 있습니다. 블루투스를 켜주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices = bluetoothAdapter!!.bondedDevices.toList()
        if (pairedDevices.isEmpty()) {
            binding.btnData.visibility = View.GONE
            Toast.makeText(requireContext(), "페어링된 기기가 없습니다", Toast.LENGTH_SHORT).show()
            return
        } else {
            binding.btnData.visibility = View.VISIBLE
        }

        adapter = BluetoothDeviceAdapter(pairedDevices, onClick = { device ->
            connectToDevice(device)
        }, connectedDevice = connectedDevice)

        binding.bluetoothDeviceList.layoutManager = LinearLayoutManager(requireContext())
        binding.bluetoothDeviceList.adapter = adapter
    }

    private fun connectToDevice(device: BluetoothDevice) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket?.connect()
                inputStream = bluetoothSocket?.inputStream

                isConnected = true
                connectedDevice = device

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "${device.name}에 연결되었습니다.", Toast.LENGTH_SHORT).show()
                    binding.btnData.isEnabled = true
                    adapter.updateConnectedDevice(device)
                }

            } catch (e: IOException) {
                Log.e(TAG, "Connection failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "연결 실패", Toast.LENGTH_SHORT).show()
                    handleDisconnection()
                    adapter.updateConnectedDevice(null)
                }
            }
        }
    }

    // Bluetooth로부터 데이터를 실시간 수신
    private fun readBluetoothData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val buffer = ByteArray(1024)
                var rawBuffer = ""

                while (isConnected) {
                    val bytes = try {
                        inputStream?.read(buffer)
                    } catch (e: IOException) {
                        withContext(Dispatchers.Main) { handleDisconnection() }
                        break
                    } ?: break

                    val dataChunk = String(buffer, 0, bytes)
                    rawBuffer += dataChunk

                    val lines = rawBuffer.split("\n")
                    rawBuffer = if (rawBuffer.endsWith("\n")) "" else lines.last()
                    val completeLines = if (rawBuffer.isEmpty()) lines else lines.dropLast(1)

                    withContext(Dispatchers.Main) {
                        for (line in completeLines.map { it.trim() }.filter { it.isNotEmpty() }) {
                            receivedBuffer.append("Received: $line\n")
                            saveToDatabase(line)  // 실시간 데이터 DB 저장
                        }

                        if (binding.textScroll.isVisible) {
                            binding.textView.text = receivedBuffer.toString()
                            scrollToBottom()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Read error", e)
                withContext(Dispatchers.Main) { handleDisconnection() }
            }
        }
    }

    // 텍스트뷰 아래로 스크롤
    private fun scrollToBottom() {
        binding.textScroll.post {
            binding.textScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun handleDisconnection() {
        isConnected = false
        bluetoothSocket?.close()
        bluetoothSocket = null
        inputStream = null
        connectedDevice = null
        adapter.updateConnectedDevice(null)

        Toast.makeText(requireContext(), "기기 연결이 끊어졌습니다", Toast.LENGTH_SHORT).show()
        binding.btnData.isEnabled = false
        binding.textView.append("\n[연결이 종료되었습니다]\n")
    }

    private fun saveToDatabase(line: String) {
        val data = Activity(
            uid = 1,
            subjectId = 1,
            activity = line.toIntOrNull() ?: -1,
            createdAt = LocalDateTime.now().toString()
        )
        dao.insert(data)
    }

    // 뷰 해제 시 리소스 정리
    override fun onDestroyView() {
        super.onDestroyView()
        unregisterBluetoothReceiver()
        bluetoothSocket?.close()
        bluetoothSocket = null
        isConnected = false
        _binding = null
    }
}

