package com.example.leavemealoneapplication

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.leavemealoneapplication.databinding.ActivityLightManagementBinding
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class LightManagement : AppCompatActivity() {
    val binding by lazy { ActivityLightManagementBinding.inflate(layoutInflater) }
    //조명 메뉴 xml 파일인, ActivityLightManagement를 inflate(메모리에 객체화 해서 올리기)
    //그러고 바인딩 하는데, 바인딩은 늦은 초기화 이용. 바인딩이 실제 쓰이는 순간에 초기화된다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Activity의 이전 상태가 포함된 Bundle 타입 객체를 이용해, Activity 생성
        // savedInstance는 Activity의 이전 상태를 저장한다. Bundle은 Map 형태로
        // 이루어진 데이터 묶음인데, 상태 저장이나 복구에 사용한다.
        setContentView(binding.root)

        var sharedLight = getSharedPreferences("lightSetting", Context.MODE_PRIVATE)
        var lightEditor = sharedLight.edit()

        CoroutineScope(Dispatchers.IO).launch {
            var lightUrlText = "http://192.168.219.110/lightSetting.json"
            var lightUrl = URL(lightUrlText)

            var lightUrlConnection = lightUrl.openConnection() as HttpURLConnection
            lightUrlConnection.requestMethod = "GET"
            lightUrlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            var lightInputStream = lightUrlConnection.getInputStream()
            var lightBuffered = BufferedReader(InputStreamReader(lightInputStream, "UTF-8"))
            var lightContent = lightBuffered.readText()
            // MainActivity에서랑 유사하게, 데이터 받아오기. 이곳에서는 Buffer에서 텍스트 꺼낼 때,
            // readText() 썼음.

            Log.d("lightResponse", "Size: " + lightContent.length)

            while (true) {
                if ((lightContent != null) && (lightContent.length != 0)) {
                    var lightJson = JSONObject(lightContent)

                    var goalLux = "${lightJson.get("goalLux")}"
                    lightEditor.putString("goalLux", "${goalLux}")
                    lightEditor.apply()

                    var chlorophyll = "${lightJson.get("chlorophyll")}"
                    lightEditor.putString("chlorophyll", "${chlorophyll}")
                    lightEditor.apply()

                    var allowingOfAUser = "${lightJson.get("allowingOfAUser")}"
                    lightEditor.putString("allowingOfAUser", "${allowingOfAUser}")
                    lightEditor.apply()
                    // 가져온 데이터를 sharedPreference에 저장.

                    break
                } else {
                    lightUrlConnection.disconnect()
                    lightBuffered.close()

                    lightInputStream = lightUrlConnection.getInputStream()
                    lightBuffered = BufferedReader(InputStreamReader(lightInputStream, "UTF-8"))
                    lightContent = lightBuffered.readText()
                    // 서버에서 데이터 가져왔으나, 내용이 비어있으면 그냥 아무것도 안함.
                }
            }

            var goalLux = sharedLight.getString("goalLux", "0")
            var chlorophyll = sharedLight.getString("chlorophyll", "A")
            var allowingOfAUser = sharedLight.getString("allowingOfAUser", "true")
            // 사용할 변수 초기화한 것. 근데 어차피 나중에 쓰일 때는,
            // 초기화 값 대신 sharedPreference 값이 들어가게 된다.

            withContext(Dispatchers.Main) {
                // UI 다루는 부분이라, Dispatcher을 main으로 바꿨음.
                // 코루틴에서 Dispatcher를 Main으로 바꾸면, UI 스레드가 이를 처리한다.

                binding.currentLuxGoalText.text = "${goalLux}" + " Lux"


                // 버튼 선택 시 UI 모양 바뀌는 것 처리.
                if (chlorophyll == "A") {
                    binding.chlorophyllBButton.selectButton(binding.less.id)
                } else if (chlorophyll == "B") {
                    binding.chlorophyllBButton.selectButton(binding.normal.id)
                } else {
                    binding.chlorophyllBButton.selectButton(binding.lots.id)
                }

                if (allowingOfAUser == "true") {
                    binding.lightOnOffToggleBtn.check(binding.on.id)
                } else {
                    binding.lightOnOffToggleBtn.check(binding.off.id)
                }
            }
        } // 서버에서 데이터 읽어오는데 필요한 코루틴 블록 끝.

        binding.saveSetting.setOnClickListener {
            // 이하 if문은 설정 저장 버튼을 누를 시, 이를 휴대폰에 파일 데이터로 저장하는 과정

            if (binding.editGoalLux.text.toString().length != 0) {
                lightEditor.putString("goalLux", binding.editGoalLux.text.toString())
                lightEditor.apply()
                Log.d("lightUpdate", "goalLux : " + "${binding.editGoalLux.text.toString()}")
            }

            // 선택된 버튼에 따라 sharedPreference에 데이터 저장.
            if (binding.less.isSelected == true) {
                lightEditor.putString("chlorophyll", "A")
                lightEditor.apply()
                Log.d("lightUpdate", "chlorophyll : A")
            } else if (binding.normal.isSelected == true) {
                lightEditor.putString("chlorophyll", "B")
                lightEditor.apply()
                Log.d("lightUpdate", "chlorophyll : B")
            } else {
                lightEditor.putString("chlorophyll", "C")
                lightEditor.apply()
                Log.d("lightUpdate", "chlorophyll : C")
            }

            if (binding.on.id == binding.lightOnOffToggleBtn.checkedId) {
                lightEditor.putString("allowingOfAUser", "true")
                lightEditor.apply()
                Log.d("lightUpdate", "allowingOfAUser : true")
            } else if (binding.off.id == binding.lightOnOffToggleBtn.checkedId) {
                lightEditor.putString("allowingOfAUser", "false")
                lightEditor.apply()
                Log.d("lightUpdate", "allowingOfAUser : false")
            }

            // 아래부터는 goalLux와 chlorophyll, allowingOfAUser 데이터 전송

            var check = true

            while(check) {

                var goalLux = sharedLight.getString("goalLux", "0")
                var chlorophyll = sharedLight.getString("chlorophyll", "A")
                var allowingOfAUser = sharedLight.getString("allowingOfAUser", "true")

                //URL의 query parameter에 데이터 담아서 전송
                var lightUrlText = "http://192.168.219.110/cgi-bin/light.py?goalLux=" +
                        "${goalLux}" + "&chlorophyll=" + "${chlorophyll}" + "&allowingOfAUser=" +
                        "${allowingOfAUser}"

                var lightUrl = URL(lightUrlText)

                var lightUrlConnection = lightUrl.openConnection() as HttpURLConnection
                lightUrlConnection.requestMethod = "GET"
                lightUrlConnection.setRequestProperty(
                    "Content-Type",
                    "text/plain"
                )

                CoroutineScope(Dispatchers.IO).launch {
                    if (lightUrlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                        Log.d("Response2","goalLux & chlorophyll & allowingOfAUser HTTP_OK")

                    } // ResponseCode를 확인하는 if문 블록 끝.
                } // 코루틴 블록 종료.
                break
            } // while(true) 블록 종료

            finish() // 현 액티비티 종료

        } // 버튼 onClickListener 블록 끝

    } // OnCreate 블록 끝
} // 액티비티 클래스 블록 끝.