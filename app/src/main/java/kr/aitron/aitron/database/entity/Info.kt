package kr.aitron.aitron.database.entity

object Info {
    // ESP32
    const val SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
    const val RX_CHARACTERISTIC_UUID_0 = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
    const val TX_CHARACTERISTIC_UUID_0 = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"

    // Teltron
    const val SERVICE_UUID_TELTRON = "0d58cf3c-3f57-11ec-9356-0242ac130003"
    const val RX_CHARACTERISTIC_UUID_TELTRON = "0d58cfe6-3f57-11ec-9356-0242ac130003"
    const val TX_CHARACTERISTIC_UUID_TELTRON = "0d58d09a-3f57-11ec-9356-0242ac130003"

    // API
    const val API_DEV = "https://api.teltron.bodywell.dev"
    const val API_PROD = "https://api.teltron.bodywell.kr"

    // Google Cloud Console - Web Client Id
    const val WEB_CLIENT_ID = "602517159097-vr04fbb5renuga4f97mk3tv8hiijm1n6.apps.googleusercontent.com"
}