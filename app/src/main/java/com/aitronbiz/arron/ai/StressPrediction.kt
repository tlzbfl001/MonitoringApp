package com.aitronbiz.arron.ai

import android.content.Context
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.common.FileUtil

class StressPrediction(context: Context) {
    private var tflite: Interpreter? = null

    init {
        try {
            // TFLite 모델 로딩
            tflite = Interpreter(FileUtil.loadMappedFile(context, "stress_model.tflite"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun predict(activity: Float, temperature: Float, lighting: Float): Float {
        // 입력 데이터 준비(활동량, 온도, 조명) 정규화
        val activityNormalized = activity / 100f
        val temperatureNormalized = temperature / 40f
        val lightingNormalized = lighting / 1000f

        val input = floatArrayOf(activityNormalized, temperatureNormalized, lightingNormalized)

        // 입력 텐서 버퍼 생성
        val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 3), DataType.FLOAT32)
        inputBuffer.loadArray(input)

        // 출력 텐서 버퍼 생성
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1), DataType.FLOAT32)

        // 예측 실행
        tflite?.run(inputBuffer.buffer, outputBuffer.buffer)

        // 예측된 스트레스 지수 (0~1 범위를 0~10 범위로 변환)
        val predictedStress = outputBuffer.floatArray[0] * 10
        return predictedStress
    }
}
