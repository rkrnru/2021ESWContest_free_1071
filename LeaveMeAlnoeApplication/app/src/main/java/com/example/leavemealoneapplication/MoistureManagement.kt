package com.example.leavemealoneapplication

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.leavemealoneapplication.databinding.ActivityLightManagementBinding
import com.example.leavemealoneapplication.databinding.ActivityMoistureManagementBinding
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MoistureManagement : AppCompatActivity() {
    val binding by lazy { ActivityMoistureManagementBinding.inflate(layoutInflater) }
    //조명 메뉴 xml 파일인, ActivityMoistureManagement를 inflate(메모리에 객체화 해서 올리기)
    //그러고 바인딩 하는데, 바인딩은 늦은 초기화 이용. 바인딩 된 게 실제 쓰이는 순간에 초기화된다.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Activity의 이전 상태가 포함된 Bundle 타입 객체를 이용해, Activity 생성
        // savedInstance는 Activity의 이전 상태를 저장한다. Bundle은 Map 형태로
        // 이루어진 데이터 묶음인데, 상태 저장이나 복구에 사용한다.

        setContentView(binding.root)

        val sharedWater = getSharedPreferences("waterSetting", Context.MODE_PRIVATE)
        val waterEditor = sharedWater.edit()

        CoroutineScope(Dispatchers.IO).launch {
            var waterUrlText = "http://192.168.219.110/waterSetting.json"
            var waterUrl = URL(waterUrlText)

            var waterUrlConnection = waterUrl.openConnection() as HttpURLConnection
            waterUrlConnection.requestMethod = "GET"
            waterUrlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            var waterInputStream = waterUrlConnection.getInputStream()
            var waterBuffered = BufferedReader(InputStreamReader(waterInputStream, "UTF-8"))
            var waterContent = waterBuffered.readText()
            // MainActivity에서랑 유사하게, 데이터 받아오기. 이곳에서는 Buffer에서 텍스트 꺼낼 때,
            // readText() 썼음.


            Log.d("waterResponse", "Size: " + waterContent.length)

            while (true) {
                if ((waterContent != null) && (waterContent.length != 0)) {
                    var waterJson = JSONObject(waterContent)

                    var humThreshold = "${waterJson.get("humThreshold")}"
                    waterEditor.putString("humThreshold", "${humThreshold}")
                    waterEditor.apply()

                    var allowingOfAUser = "${waterJson.get("allowingOfAUser")}"
                    waterEditor.putString("allowingOfAUser", "${allowingOfAUser}")
                    waterEditor.apply()
                    // 가져온 데이터를 sharedPreference에 저장.
                    break

                } else {
                    waterUrlConnection.disconnect()
                    waterBuffered.close()

                    waterInputStream = waterUrlConnection.getInputStream()
                    waterBuffered = BufferedReader(InputStreamReader(waterInputStream, "UTF-8"))
                    waterContent = waterBuffered.readText()
                    // 서버에서 데이터 가져왔으나, 내용이 비어있으면 연결 종료.
                    // 좌측 변수에 저장하는 값은 이후에 쓰이지 않는다.
                }
            }

            var humThreshold = sharedWater.getString("humThreshold", "0")
            var allowingOfAUser = sharedWater.getString("allowingOfAUser", "true")

            withContext(Dispatchers.Main) {
                // UI 다루는 부분이라, Dispatcher을 main으로 바꿨음.
                // 코루틴에서 Dispatcher를 Main으로 바꾸면, UI 스레드가 이를 처리한다.
                binding.currentMoistureGoalAsPercent.text = "${humThreshold}" + "%"

                // 선택된 버튼에 따라 UI 변경
                if (allowingOfAUser == "true") {
                    binding.waterOnOffToggleBtn.check(binding.on.id)
                } else {
                    binding.waterOnOffToggleBtn.check(binding.off.id)
                }
            }
        } // 서버에서 데이터를 읽어오는 데 필요한 코루틴 블록 끝.

            binding.saveMoistureSetting.setOnClickListener {
                // 이하의 if문은 설정 저장 버튼을 누를 시, 이를 휴대폰에 파일 데이터로 저장하는 과정.

                if(binding.editThreshold.text.toString().length != 0){
                    waterEditor.putString("humThreshold", binding.editThreshold.text.toString())
                    waterEditor.apply()
                    Log.d("waterUpdate","humThreshold : " + "${binding.editThreshold.text.toString()}")
                }

                if(binding.on.id == binding.waterOnOffToggleBtn.checkedId){
                    waterEditor.putString("allowingOfAUser","true")
                    waterEditor.apply()
                    Log.d("waterUpdate", "allowingOfAUser : true")
                } else if(binding.off.id == binding.waterOnOffToggleBtn.checkedId){
                    waterEditor.putString("allowingOfAUser","false")
                    waterEditor.apply()
                    Log.d("waterUpdate","allowingOfAUser : false")
                }

                // 아래부터는 humThreshold와 allowingOfAUser 데이터 전송

                while(true) {

                    // 사용자가 변경한 데이터를 서버에 Query String을 이용해서 전송
                    var humThreshold = sharedWater.getString("humThreshold", "0")
                    var allowingOfAUser = sharedWater.getString("allowingOfAUser", "true")

                    var waterUrlText =
                        "http://192.168.219.110/cgi-bin/water.py?humThreshold=" + "${humThreshold}" +
                                "&allowingOfAUser=" + "${allowingOfAUser}"

                    var waterUrl = URL(waterUrlText)

                    var waterURLConnection = waterUrl.openConnection() as HttpURLConnection
                    waterURLConnection.requestMethod = "GET"
                    waterURLConnection.setRequestProperty(
                        "Content-Type",
                        "text/plain"
                    )

                    // Response Code 확인
                    CoroutineScope(Dispatchers.IO).launch {
                        if (waterURLConnection.responseCode == HttpURLConnection.HTTP_OK) {
                            Log.d("Response1","HumThreshold & allowingOfAUser HTTP_OK")
                        }
                    }
                    break
                }// while(true) 블록 종료

                finish() // 현 액티비티 종료

            } // saveSetting 버튼 OnClickListener 블록 끝

        } // OnCreate 블록 끝
    } // 액티비티 클래스 블록 끝
