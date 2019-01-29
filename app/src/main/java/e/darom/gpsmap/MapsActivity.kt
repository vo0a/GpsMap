package e.darom.gpsmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationProvider
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.util.Log
import android.view.WindowManager
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

/*
에뮬레이터에서 테스트하기

에뮬레이터를 실행하고 ... 을 클릭하여 Extenede controls 화면을 엽니다. Location 탭을 클릭하면 GPS를 가상으로 테스트하는 화면이 표시됩니다.

이동 경로에 해당하는 좌표 정보를 가지고 있으면 GPS를 테스트하기 편리합니다. 오픈 스트리트맵에서 공개된 GPS 이동 경로를 내려받을 수 있습니다.
-https://www.openstreetmap.org/
먼저 오픈스트리트맵 사이트에 접속하여 'GPS 궤적'을 클릭합니다.

공개 GPS 궤적 목록이 표시됩니다. 점 개수가 많을수록 용량이 크니 점 개수가 1,000개 이하인 적당한 파일을 내려받습니다.
에뮬레이터에서 사용하려면 gpx 확장자의 파일을 받는 것이 좋습니다.

LOAD GPX/KML 을 클릭하여 내려받은 파일을 선택하면 위치 정보 데이터가 표시됩니다. 실행 버튼을 클릭하면 좌표를 순서대로 에뮬레이터에 전송합니다.
좌표 전송 간격을 빠르게 사려면 속도를 5배까지 조절할 수 있습니다.

 */

/*
    activitiy_maps.xml 레이아웃 파일을 열어보면 화면에 꽉 찬 프래그먼트에 속성 창을 보면 이름이 지정되어있는데
    이것은 구글 지도가 내장된 프래그먼트로 play-services-maps 라이브러리에서 제공됩니다.

    implementation 'com.google.android.gms:play-services-maps:16.0.0'

*/
/*
위치 정보 요청

구글 플레이 서비스를 최신 버전으로 업데이트해야 위치 서비스에 연결됩니다.
위치 서비스에 연결된 앱은 FusedLocationProviderClient 클래스의 requestLocationUpdates() 메서드를 호출하여 위치 정보를 요청할 수 있습니다.

requestLocationUpdates(locationRequest: LocationRequest, locationCallback: LocationCallback, looper: Looper)

-locationRequest : 위치 요청 객체입니다.
-locationCallback : 위치가 갱신되면 호출되는 콜백입니다.
-looper : 특정 루퍼 스레드를 지정합니다. 특별한 경우가 아니라면 null을 지정합니다.

위치 정보를 주기적으로 요청하는 코드는 액티비티가 화면에 보일 때만 수행하는 것이 좋습니다. onResume() 콜백 메서드에서 위치 정보 요청을 수행하고,
onPause() 콜백 메서드에서 위치 정보 요청을 삭제하는 것이 일반적인 방법입니다.

requestLocationUpdates() 메서드의 첫 번째 인자인 LocationRequest 객체는 위치 정보를 요청하는 시간 주기를 설정하는 객체입니다.
 */
/*
1. 위치 정보를 주기적으로 얻는 데 필요한 객체들을 선언합니다. MyLocationCallback 은 MapsActivitiy 클래스의 내부 클래스로 생성했습니다.

1. 에서 선언한 변수들은 onCreate() 메서드 2. 끝에서 초기화합니다.

3. LocationRequest 는  위치 정보 요청에 대한 세부 정보를 설정합니다. 여기에 설정하는 프로퍼티의 의미를 살펴보면 다음과 같습니다.
-priority : 정확도를 나타냅니다.
    PRIORITY_HIGH_ACCURACY : 가장 정확한 위치를 요청합니다.
    PRIORITY_BALANCED_POWER : '블록' 수준의 정확도를 요청합니다.
    PRIORITY_LOW_POWER : '도시' 수준의 정확도를 요청합니다.
    PRIORITY_NO_POWER : 추가 전력 소모 없이 최상의 정확도를 요청합니다.

4. 이러한 위치 요청은 액티비티가 활성화되는 onResume() 메서드에서 수행하며 5. 와 같이 별도의 메서드로 작성합니다.

6. requestLocationUpdates() 메서드에 전달되는 인자 중 LocationCallBack 을 구현한 내부 클래스는
LocationResult 객체를 반환하고 lastLocation 프로퍼티로 Location 객체를 얻습니다.

7. 기기의 GPS 설정이 꺼져 있거나 현재 위치 정보를 얻을 수 없을 경우에 Location 객체가 null 일 수 있습니다.
Location 객체가 null 이 아닐 때 해당 위도와 경도 위치로 카메라를 이동합니다.
 */
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    // 1.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: MyLocationCallBack

    private val REQUEST_ACCESS_FINE_LOCATION = 1000

    // PolyLine 옵션
    private val polylineOptions = PolylineOptions().width(5f).color(Color.RED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 화면 유지하기
        /*
         8장 수평 측정기와 같이 지도를 테스트할 때 화면이 돌아가거나 자도으로 꺼지면 테스트하기가 어렵습니다.
         다음과 같이 화면 방향을 고정하고, 화면이 자동으로 꺼지지 않도록 코드를 추가합니다.
         */
        // 화면이 꺼지지 않게 하기
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 세모 모드로 화면 고정
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        // SupportMapFragment 를 가져와서 지도가 준비되면 알림을 받습니다.     //1.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 2.
        locationInit()
    }

    // 3. 위치 정보를 얻기 위한 각종 초기화
    private fun locationInit() {
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        locationCallback = MyLocationCallBack()

        locationRequest = LocationRequest()
        // GPS 우선
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        // 업데이트 인터벌
        // 위치 정보가 없을 때는 업데이트 안 함
        // 상황에 따라 짧아질 수 있음, 정확하지 않음
        // 다른 앱에서 짧은 인터벌로 위치 요청하면 짧아질 수 있음
        locationRequest.interval = 10000
        // 정확함. 이것보다 짧은 업데이트는 하지 않음
        locationRequest.fastestInterval = 5000
    }


    /**
     * 사용 가능한 맵을 조작합니다.
     * 지도를 사용할 준비가 되면 이 콜백이 호출됩니다.
     * 여기서 마커나 선, 청취자를 추가하거나 카메라를 이동할 수 있습니다.
     * 호주 시드니 근처에 마커를 추가하고 있습니다.
     * Google Play 서비스가 기기에 설치되어 있지 않은 경우 사용자에게
     * SupportMapFragment 안에 Google Play 서비스를 설치하고 앱으로 돌아온 후에만 호출(혹은 실행)됩니다.
     *
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    /*
    1. 프래그먼트 매니저로부터 SupportMapFragment 를 얻습니다. getMapAsync() 메서드로 지도가 준비되면 알림을 받습니다.

    2. 지도가 준비되면 GoogleMap 객체를 얻습니다.

    3. 위도와 경도로 시드니의 위치를 정하고 구글 지도 객체에 마커를 추가하고 카메라를 이동합니다.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap    // 2.

        // Add a marker in Sydney and move the camera   시드니에 마커를 추가하고 카메라를 이동합니다. // 3.
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    // 4. addLocationListener
    /*
     11. 메서드를 호출하기 전에 9. 메서드를 이용하여 권한을 요청합니다. 첫 번째 인자인 cancel() 함수는 이전에 사용자가 권한 요청을 거부했을 때 호출됩니다.
     10. 권한이 필요한 이유를 알려주는 다이얼로그를 표시합니다. 두 번째 인자인 ok() 함수는 사용자가 권한을 수락했을 때 호출됩니다. 11. 주기적인 위치 정보 갱신을 시작합니다.

     하지만 여전히 addLocationListener() 내부에는 빨간불이 꺼지지 않습니다.
     안드로이드 스튜디오에는 9장에서 처럼 권한이 필요한 코드의 주변에 직접 작성한 권한 요청 코드만 인식하기 때문입니다.
     Alt + Enter 를 누르고 이 메서드에서는 권한 요청 에러를 표시하지 않도록 하는 Suppress: Add @suppressList("MissingPermission") annotation 을 클릭합니다.

     권한 요청 코드를 제대로 작성했지만 별도의 메서드로 해당 코드 블록을 분리하면 안드로이드 스튜디오가 에러로 판단합니다. 당황하지 말고 에러 표시를 없애기 바랍니다.
     */
    override fun onResume() {
        super.onResume()

        // 9. 권한 요청
        permissionCheck(cancel = {
            // 10. 위치 정보가 필요한 이유 다이얼로그 표시
            showPermissionInfoDialog()
        }, ok = {
            // 11. 현재 위치를 주기적으로 요청( 권한이 필요한 부분)
            addLocationListener()   // 4.
        })
    }

    // 5. 위치 권한 요청 : addLocationListener() 메서드에 빨간 줄로 에러 표시가 나는 이유는 실행 중 권한 요청 코드를 작성하지 않았기 때문입니다.
    // 9장에서와 달리 권한을 요청하는 부분이 여러 군데일 때는 권한 요청 코드를 각각 작성해야만 합니다.
    @SuppressLint("MissingPermission")
    private fun addLocationListener(){
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    // 6.
    inner class MyLocationCallBack : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)

            val location = locationResult?.lastLocation
            // 7.
            location?.run {
                // 14 level 로 확대하고 현재 위치로 카메라 이동
                val latLng = LatLng(latitude, longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))


                // 1. 위치 정보 갱신 확인하기 : Location 객체의 위도와 경도값을 로그로 출력합니다.
                // 앱을 실행하여 Logcat 탭을 클릭하고 MapActivity 로 필터링하여 위도와 경도 정보가 잘 표시되는지 확인합니다.
                Log.d("MapActivity", "위도: $latitude, 경도: $longitude")

                // 이동 자취를 선으로 그리기
                /*
                구글 지도는 이동 자취를 그리는 다양한 메서드를 제공합니다.
                - addPolyLine() : 선의 집합으로 지도에 경로와 노선을 표시합니다.
                - addCircle() : 원을 표시합니다.
                - addPolygon() : 영역을 표시합니다.

                여기서는 addPolyLine() 메서드를 사용해서 이동 자취를 그리겠습니다.

                구현 순서는 다음과 같습니다.
                1. 이동 경로 그리기
                2. 화면 유지하기
                3. 에뮬레이터에서 테스트하기

                먼저 PolylineOption() 객체를 생성합니다. 선을 이루는 좌표들과 선의 굵기, 색상 등을 설정할 수 있습니다. 여기서는 굵기 5f, 색상은 빨강으로 설정했습니다.
                위치 정보가 갱신되면 해당 좌표를 2. polyLineOption 객체에 추가합니다. 3. 지도에 polylineOption 객체를 추가합니다.

                이제 앱을 실행해서 이동해보세요. 이동 경로가 빨간 선으로 잘 표시되면 성공입니다.
                 */

                // 2. PolyLine 에 좌표 추가
                polylineOptions.add(latLng)

                // 3. 선 그리기
                mMap.addPolyline(polylineOptions)
            }

        }
    }

    //실행 중 권한 요청 메서드 작성
    /*
     1. 이 메서드는 함수 인자 두 개를 받습니다. 두 함수는 모두 인자가 없고 반환값도 없습니다.
     이전에 사용자가 권한 요청을 거부한 적이 있다면 2. cancle() 함수를 호출하고, 권한이 수락되었다면 3. ok() 함수를 호출합니다.
     Manifest 클래스는 android.Manifest 를 임포트하는 것에 주의합니다.

     먼저 cancel() 함수에 해당하는 메서드를 작성하겠습니다. 사용자가 한 번 거부했을 때 권한이 필요한 이유를 설명하는 다이얼로그를 표시하는 메서드를
     Anko 라이브러리를 활용하여 다음과 같이 작성합니다.
     */
    private fun permissionCheck (cancel: () -> Unit, ok: () -> Unit) =// 1.
    //위치 권한이 있는지 검사
        if ( checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            // 권한이 허용되지 않음
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                // 이전에 권한을 한 번 거부한 적이 있는 경우에 실행할 함수
                cancel() // 2.
            } else{
                // 권한 요청
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_ACCESS_FINE_LOCATION)
            }
        } else{
            // 권한을 수락했을 때 실행할 함수
            ok()
        }
    /*
    이 메서드는 4. 위치 정보가 필요한 이유를 설명하는 ALERDialog 를 8. 표시합니다.
    이 다이얼로그는 5. 긍정 버튼과 7. 부정 버튼을 가지고 있고, 5. 긍정 버튼을 누르면 권한을 요청하고, 7. 부정 버튼을 누르면 아무것도 하지 않고 다이얼로그가 닫힙니다.

    requestPermission() 메서드의 6. 첫 번째 인자는 Context Activity 를 전달하는데, yesButton{} 블럭 내부에서 this 는 DialogInterFace 객체를 나타냅니다.
    따라서 액티비티를 명시적으로 가리키려면 this@MapsActivity 로 작성해야 합니다.
     */
    private fun showPermissionInfoDialog() {
        alert("현재 위치 정보를 얻으려면 위치 권한이 필요합니다", "권한이 필요한 이유") { // 4.
            yesButton {  // 5.
                ActivityCompat.requestPermissions(this@MapsActivity, // 6.
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_ACCESS_FINE_LOCATION)
            }
            noButton {  } // 7.
        }.show() // 8.
    }

    // 권한 선택에 대한 처리 : 위치 갱신 or 메시지 출력
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            REQUEST_ACCESS_FINE_LOCATION -> {
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // 1. 권한 허용됨 : addLocationListener() 메서드를 호출하여 위치 정보를 갱신합니다.
                    addLocationListener()
                } else{
                    // 2. 권한 거부
                    toast("권한 거부 됨")
                }
                return
            }
        }
    }

    // 위치 정보 요청 삭제
    /*
    1. 위치 요청을 취소합니다. 2. 메서드에서 LocationCallback 객체를 전달하여 주기적인 위치 정보 갱신 요청을 삭제합니다.

    이제 권한 요청 및 위치 정보 요청의 추가와 삭제까지 구현했습니다. 앱을 실행하여 권한 요청이 제대로 동작되고 수락했을 때 현재 위치로 지도가 이동되면 성공입니다.
    이때 기기의 위치 기능이 켜져 있어야 제대로 작동되니 잊지 말고 미리 위치 기능을 켜두세요.
     */
    override fun onPause() {
        super.onPause()
        // 1.
        removeLocationListener()
    }
    private fun removeLocationListener() {
        // 2. 현재 위치 요청을 삭제
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

}
