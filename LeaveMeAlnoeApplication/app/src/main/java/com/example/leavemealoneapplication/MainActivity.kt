package com.example.leavemealoneapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.leavemealoneapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {
    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        // 바인딩 가능하게 설정

        //main 화면에서 표시되는 광도, 습도 update 할 때 쓰는 함수
        suspend fun updateUI(currentLuxMessage:String, humidityMessage:String){
            binding.mainCurrentLux.text = currentLuxMessage
            binding.mainCurrentHumidity.text = humidityMessage
        }

        //sharedPreference 이용해서 값 저장.
        val sharedLight = getSharedPreferences("lightSetting", Context.MODE_PRIVATE)
        val sharedWater = getSharedPreferences("waterSetting", Context.MODE_PRIVATE)

        // 위에 만든 sharedPreference 값을 수정할 때 쓰는 edit 객체
        val lightEditor = sharedLight.edit()
        val waterEditor = sharedWater.edit()


        // UI 그리는 기본 스레드 방해하지 않게, 코루틴 블럭으로 통신 실행.
        // Dispatchers.IO : 네트워크, 디스크 사용 할때 사용한다.
        // 파일 읽고, 쓰고, 소켓을 읽고, 쓰고 작업을 멈추는것에 최적화되어 있다.
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                if (isWiFiAvailable(applicationContext)) {
                    var lightUrlText = "http://192.168.0.34/lightSetting.json"
                    var waterUrlText = "http://192.168.0.34/waterSetting.json"

                    var lightUrl = URL(lightUrlText)
                    var waterUrl = URL(waterUrlText)

                    var lightUrlConnection = lightUrl.openConnection() as HttpURLConnection
                    lightUrlConnection.requestMethod = "GET"
                    lightUrlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    // connection 열고, GET 메소드 쓰기로 하고 상기의 Property를 써서 데이터 요청하기로 한다.
                    // RequestProperty는 HTTP header에 들어가며, 위에 적은 건, json 타입의 content를 가져오며,
                    // UTF-8 encoding을 요청하는 거다.

                    var waterUrlConnection = waterUrl.openConnection() as HttpURLConnection
                    waterUrlConnection.requestMethod = "GET"
                    waterUrlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                    //responseCode 체크
                    var response = lightUrlConnection.responseCode
                    Log.d("response==", response.toString())

                    var response2 = waterUrlConnection.responseCode
                    Log.d("response==", response.toString())

                    var lightInputStream = lightUrlConnection.getInputStream()
                    var lightBuffered = BufferedReader(InputStreamReader(lightInputStream, "UTF-8"))
                    var lightContent = StringBuilder()
                    // InputStream 가져오고, Buffer에 넣어서 읽는다.

                    try {
                        var line = lightBuffered.readLine()
                        while(line != null){
                            lightContent.append(line)
                            line = lightBuffered.readLine()
                        }
                    } finally {
                        lightBuffered.close()
                    }
                    // Buffer에 담긴 걸 StringBuilder 객체에 하나씩 넣는다.

                    var waterInputStream = waterUrlConnection.getInputStream()
                    var waterBuffered = BufferedReader(InputStreamReader(waterInputStream, "UTF-8"))
                    var waterContent = StringBuilder()

                    try {
                        var line = waterBuffered.readLine()
                        while(line != null){
                            waterContent.append(line)
                            line = waterBuffered.readLine()
                        }
                    } finally {
                        waterBuffered.close()
                    }

                    // 수분 데이터도 위와 동일하게 처리한다.

                    Log.d("lightResponse", "Size: " + lightContent.length)
                    Log.d("waterResponse", "Size: " + waterContent.length)
                    Log.d("content", "${lightContent}")

                    fun safeLightCall(lightContent: StringBuilder): String? {
                        var string = lightContent.toString()
                        if ((string != null) && (string.isNotEmpty())) {
                            var lightJson = JSONObject(string)
                            var currentLux = "${lightJson.get("currentLux")}"
                            return currentLux
                        } else {
                            return null
                        }
                    }
                    // 서버에서 받아온 JSON 데이터가 비어있진 않았는지 확인하고,
                    // 데이터에서 현재 조도만 추출해서 currentLux 변수에 보관한다.

                    var currentLux: String? = safeLightCall(lightContent)
                    var currentLuxMessage: String?
                    currentLuxMessage = null
                    if (currentLux != null) {
                        currentLuxMessage = "현재 조도 : " + currentLux + " Lux"
                    }
                    // 조도 데이터에 문제 없으면 UI에 표시되는 조도 메시지를 변경한다.

                    fun safeWaterCall(waterContent: StringBuilder): String? {
                        var string = waterContent.toString()
                        if ((string != null) && (string.isNotEmpty())) {
                            var waterJson = JSONObject(string)
                            var humidity = "${waterJson.get("humidity")}"
                            return humidity
                        } else {
                            return null
                        }
                    }

                    var humidity: String? = safeWaterCall(waterContent)
                    var humidityMessage: String?
                    humidityMessage = null
                    if (humidity != null) {
                        humidityMessage = "현재 습도 : " + humidity + "%"
                    }
                    // 습도 데이터도 조도와 마찬가지로 처리한다.

                    if ((currentLuxMessage != null) && (humidityMessage != null)) {
                        withContext(Dispatchers.Main) {
                            updateUI(currentLuxMessage, humidityMessage)
                        }
                    }

                    lightEditor.putString("currentLux", "${currentLux}")
                    lightEditor.apply()
                    waterEditor.putString("humidity", "${humidity}")
                    waterEditor.apply()
                    // UI에 변경해 넣은 데이터를 sharedPreference에도 editor를 이용해 저장한다.


                    lightBuffered.close()
                    waterBuffered.close()

                    lightUrlConnection.disconnect()
                    waterUrlConnection.disconnect()

                    delay(850L)
                } else {
                    var currentLux = sharedLight.getString("currentLux", "기본값")
                    var humidity = sharedWater.getString("humidity", "기본값")

                    var currentLuxMessage = "현재 조도 : " + currentLux + " Lux"
                    var humidityMessage = "현재 습도 : " + humidity + "%"

                    withContext(Dispatchers.Main) {
                        binding.mainCurrentLux.text = currentLuxMessage
                        binding.mainCurrentHumidity.text = humidityMessage
                    }
                    // 통신이 안됐을 때 구문. 그냥 무의미한 텍스트를 띄운다.
                }
            }
        }

        val moistureIntent = Intent(this, MoistureManagement::class.java)
        val lightIntent = Intent(this, LightManagement::class.java)
        val communicationIntent = Intent(this, Communication::class.java)
        // 다른 메뉴로 진입할 때, 넘겨주는 Intent이다.

        binding.mainMoisture.setOnClickListener { startActivity(moistureIntent) }
        binding.mainLight.setOnClickListener { startActivity(lightIntent) }
        binding.mainCommunication.setOnClickListener { startActivity(communicationIntent) }
        // 다른 메뉴 진입용 버튼 바인딩
    }
}
