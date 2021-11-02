package com.example.leavemealoneapplication

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class Communication : AppCompatActivity() {

    //lateinit var networkConnectionStateMonitor: NetworkConnectionStateMonitor
    lateinit var button: Button
    // lateinit은 속성(property), 즉 클래스 안의 변수를 선언 시 초기화하지 않아도 되게 한다.
    // 버튼 하나 만들기

    lateinit var networkConnectionStateMonitor: NetworkConnectionStateMonitor
    // NetworkConnectionStateMonitor라는 클래스로 객체 1개 생성.
    // 그리고 lateinit.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communication)
        // setContentView는 원하는 content를 display 할 때 사용한다.
        // layout xml의 내용을 parsing하여, view를 생성하고, view에 정의된 속성을 설정한다.
        // R은 res 폴더를 의미하고, layout은 R의 내부 클래스, activity_communication은 같은
        // 이름의 xml 파일을 의미한다.
        // 그래서 R.layout.activity_communication은 activity_communication.xml을
        // 가리키는 ID이다.

        // 안드로이드 스튜디오는 변수를 가지고, xml 파일을 가리킬 수 있다. 스튜디오에서
        // 리소스 관리를 해주기 때문이다.

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            networkConnectionStateMonitor = NetworkConnectionStateMonitor(this)
            // 현재 device의 SDK 버전이 LOLLIPOP(2014년) 이후면
            // 현재 context로 NetworkConnectionStateMonitor 객체 생성
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            networkConnectionStateMonitor.register()
            // 마찬가지로 LOLLIPOP 이후 버전이면, register() 함수 실행.
        }

        button = findViewById(R.id.communcationBtn)
        button.setOnClickListener(listener)
        // 버튼의 ID 찾아서, ClickListener 연결, 버튼 누를 시 listener에 연결된 코드 실행

        var WiFiConnetion01: ImageView

        WiFiConnetion01 = findViewById(R.id.connected)
        // ImageView 만든다. 그리고 연결 상태를 나타내는 image id를 등록해둔다.

        var isWiFi = isWiFiAvailable(this)

        if(isWiFi){
            Log.d("WiFI", "WiFi On")
            WiFiConnetion01.setImageResource(R.drawable.connected)
        }
        else{
            Log.d("WiFI", "WiFi Off")
            WiFiConnetion01.setImageResource(R.drawable.unconnected)
        }
        // WiFi 연결 상태에 따라, image view 변경.
    } //onCreate() 함수 override 블록의 끝

    val listener = View.OnClickListener {    view ->
        when(view.getId()){ // view가 존재하면 실행.
            R.id.communcationBtn -> {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                // 새 Activity 실행. Intent()를 이용해 실행될 Activity를 지정한다.
                // ACTION_WIFI_SETTINGS는 Wi-Fi 환경 설정을 다루는 곳을 보여주는
                // Activity Action이다.
                // 해당 사항을 Intent에 지정하여 startActivity를 이용하면
                // Wi-Fi 환경설정 창이 열린다.
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        // 사용자가 기존 Activity를 멈추고 다른 일을 하다가, 되돌아 왔을 때 onRestart()가
        // 실행된다. 이를 override하면, restart 시 어떤 동작을 할지 정할 수 있다.
        // onRestart를 재정의할 때는, superclass를 통한 호출문을 반드시 넣어야 정상 동작한다.
        // 그래서 super.onRestart()로 코드를 시작한다.

        var WiFiConnection02: ImageView
        WiFiConnection02 = findViewById(R.id.connected)

        var isWiFi = isWiFiAvailable(this)

        if(isWiFi){
            Log.d("WiFI", "WiFi On")
            WiFiConnection02.setImageResource(R.drawable.connected)
        }
        else{
            Log.d("WiFI", "WiFi Off")
            WiFiConnection02.setImageResource(R.drawable.unconnected)
        }
        // 사용자가 Wi-Fi 설정을 마치고 다시 Activity로 돌아왔을 때, 새 연결 상태에
        // 따라서 ImageView를 변경한다.
    }
}




fun isWiFiAvailable(context: Context?) :Boolean {
    if (context == null) return false
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                return true
        }
    }
    return false
}


