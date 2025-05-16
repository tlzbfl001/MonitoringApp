package kr.aitron.aitron

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.aitron.aitron.entity.Device
import kr.aitron.aitron.entity.EnumData
import kr.aitron.aitron.entity.Subject
import kr.aitron.aitron.util.CustomUtil.TAG
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDateTime

class MainViewModel(application: Application) : AndroidViewModel(application) {
    /*private val repository = DatabaseRepository(application)

    private val _subjectLiveData = MutableLiveData<Subject?>(null)
    val subjectLiveData: LiveData<Subject?> get() = _subjectLiveData

    private val _subjectInsertedLiveData = MutableLiveData<Boolean>(false)
    val subjectInsertedLiveData: LiveData<Boolean> get() = _subjectInsertedLiveData

    private val _deviceListLiveData = MutableLiveData<List<Device>>(emptyList())
    val deviceListLiveData: LiveData<List<Device>> get() = _deviceListLiveData

    private val _deviceInsertedLiveData = MutableLiveData<Boolean>(false)
    val deviceInsertedLiveData: LiveData<Boolean> get() = _deviceInsertedLiveData

    private val _handleScannedQRResult = MutableLiveData<EnumData>()
    val handleScannedQRResult: LiveData<EnumData> get() = _handleScannedQRResult

    fun handleScannedQRCode(code: String, subjectId: Int, uid: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            var resultType = EnumData.UNKNOWN

            Log.d(TAG, "handleScannedQRCode code: $code")
            try {
                val data = JSONObject(code)
                val productNumber = data.getString("0")
                val serialNumber = data.getString("1")

                val device = Device(
                    uid = uid,
                    subjectId = subjectId,
                    name = "",
                    productNumber = productNumber,
                    serialNumber = serialNumber,
                    createdAt = LocalDateTime.now().toString()
                )

                val deviceId = repository.insertDevice(device)
                if (deviceId > 0) {
                    resultType = EnumData.DONE
                }
            }catch (error: JSONException) {
                Log.e(TAG, "JSON Parsing Error: $error")
                resultType = EnumData.UNKNOWN
            }

            withContext(Dispatchers.Main) {
                _handleScannedQRResult.postValue(resultType)
            }
        }
    }

    fun getSubjectByUid(uid: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val subject = repository.getSubjectByUid(uid)
            Log.d(TAG, "subject: $subject")
            withContext(Dispatchers.Main) {
                if(subject != null) {
                    Log.d(TAG, "subject.id: ${subject.id}")
                    _subjectLiveData.postValue(subject)
                }else {
                    Log.d(TAG, "subject.id: null")
                    _subjectLiveData.postValue(null)
                }
            }
        }
    }

    fun insertSubject(subject: Subject) {
        viewModelScope.launch {
            val success = repository.insertSubject(subject) > 0
            withContext(Dispatchers.Main) {
                _subjectInsertedLiveData.postValue(success)
            }
        }
    }

    fun insertDevice(device: Device) {
        viewModelScope.launch {
            val success = repository.insertDevice(device) > 0
            withContext(Dispatchers.Main) {
                _deviceInsertedLiveData.postValue(success)
            }
        }
    }

    fun resetInsertState() { // true일 때 한 번만 실행되도록 reset
        _subjectInsertedLiveData.postValue(false)
        _deviceInsertedLiveData.postValue(false)
        _subjectLiveData.postValue(null)
        _handleScannedQRResult.postValue(EnumData.UNKNOWN)
    }*/
}