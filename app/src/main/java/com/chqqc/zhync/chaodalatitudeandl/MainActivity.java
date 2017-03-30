package com.chqqc.zhync.chaodalatitudeandl;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.BDNotifyListener;//假如用到位置提醒功能，需要import该类
import com.baidu.location.Poi;
import com.chqqc.zhync.chaodalatitudeandl.entity.LocaInfo;
import com.chqqc.zhync.chaodalatitudeandl.handle.MainHandler;

import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final int SDK_PERMISSION_REQUEST = 127;
    private WebView mWebView;
    private TextView latitude, longitude,speed,area;
    private FloatingActionButton fab;

    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyLocationListener();
    private  int randomNum;
    private String permissionInfo;
    private Realm realm;
    private RealmConfiguration realmConfig;
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        randomNum = 0;
        initRandom();
        setContentView(R.layout.activity_main);

        initRealm();
        mWebView = (WebView) findViewById(R.id.webview);
        // 启用javascript
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.addJavascriptInterface(new MyJavaScriptInterface(this), "mlocation"); //设置js接口  第一个参数事件接口实例，第二个是实例在js中的别名，这个在js中会用到
        String url = "http://wfarm.test.foodmall.com/mobile/src/html/emap/run_path_iframe.html?r="+randomNum;
        mWebView.loadUrl(url);

        //覆盖WebView默认使用第三方或系统默认浏览器打开网页的行为，使网页用WebView打开
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub
                //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
                view.loadUrl(url);
                return true;
            }

        });

        mWebView.setWebChromeClient(new WebChromeClient());


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);

        mLocationClient = new LocationClient(getApplicationContext());
        //声明LocationClient类
        mLocationClient.registerLocationListener( myListener );
        //注册监听函数

        getPersimmions();
        initEvent();

        initLocation();
        LocationManager locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        if(!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){

            // 未打开位置开关，可能导致定位失败或定位不准，提示用户或做相应处理

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage("请打开GPS");
            dialog.setPositiveButton("确定",
                    new android.content.DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {

                            // 转到手机设置界面，用户设置GPS
                            Intent intent = new Intent(
                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, 0); // 设置完成后返回到原来的界面

                        }
                    });
            dialog.setNeutralButton("取消", new android.content.DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    arg0.dismiss();
                }
            } );
            dialog.show();
        }


        mLocationClient.start();
        //timer.schedule(task, 1000, 2000); // 1s后执行task,经过1s再次执行

    }

    /**
     * 初始化Realm
     */
    private void initRealm() {
        byte[] key = new byte[64];
        new SecureRandom().nextBytes(key);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .encryptionKey(key)
                .build();

        // Start with a clean slate every time
        Realm.deleteRealm(realmConfiguration);

        // Open the Realm with encryption enabled
        realm = Realm.getInstance(realmConfiguration);
    }


    @TargetApi(23)
    private void getPersimmions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();
            /***
             * 定位权限为必须权限，用户如果禁止，则每次进入都会申请
             */
            // 定位精确位置
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
			/*
			 * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
			 */
            // 读写权限
            if (addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionInfo += "Manifest.permission.WRITE_EXTERNAL_STORAGE Deny \n";
            }
            // 读取电话状态权限
            if (addPermission(permissions, Manifest.permission.READ_PHONE_STATE)) {
                permissionInfo += "Manifest.permission.READ_PHONE_STATE Deny \n";
            }

            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
            }
        }
    }

    @TargetApi(23)
    private boolean addPermission(ArrayList<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
            if (shouldShowRequestPermissionRationale(permission)){
                return true;
            }else{
                permissionsList.add(permission);
                return false;
            }

        }else{
            return true;
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }



    Handler handler = new Handler() {
           public void handleMessage(Message msg) {
               if (msg.what == 1) {
                   //tvShow.setText(Integer.toString(i++));
                   mLocationClient.start();
               }
               super.handleMessage(msg);
           };
       };
       Timer timer = new Timer();
       TimerTask task = new TimerTask() {

           @Override
           public void run() {
               // 需要做的事:发送消息
               Message message = new Message();
               message.what = 1;
               handler.sendMessage(message);
           }
       };


    private void initRandom() {
        Random dom = new Random();
        int ints = dom.nextInt(100000);//6位
        if(ints<10000){//小于6位的判断,可以取出非常整齐的全是6位的随机数
           randomNum = ints;
        }else{
            randomNum = ints;
        }
    }

    //定位获取信息
    private void initLocation() {

        //获取显示地理位置信息的TextView
        latitude = (TextView) findViewById(R.id.Latitude);
        longitude = (TextView) findViewById(R.id.longitude);
        speed = (TextView) findViewById(R.id.speed);
        area = (TextView) findViewById(R.id.area);

        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备

        option.setCoorType("bd09ll");
        //可选，默认gcj02，设置返回的定位结果坐标系

        int span=1000;
        option.setScanSpan(span);
        //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的

        option.setIsNeedAddress(true);
        //可选，设置是否需要地址信息，默认不需要

        option.setOpenGps(true);
        //可选，默认false,设置是否使用gps

        option.setLocationNotify(true);
        //可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果

        option.setIsNeedLocationDescribe(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”

        option.setIsNeedLocationPoiList(true);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到

        option.setIgnoreKillProcess(false);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死

        option.SetIgnoreCacheException(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集

        option.setEnableSimulateGps(false);
        //可选，默认false，设置是否需要过滤GPS仿真结果，默认需要

        mLocationClient.setLocOption(option);

    }

    private void initEvent() {
        fab.setOnClickListener(this);
    }


    /**
     * 显示地理位置经度和纬度信息
     * @param location
     */
    private void showLocation(final BDLocation location) {
        String locationStr = "维度：" + location.getLatitude() + "\n"
                + "经度：" + location.getLongitude();

        latitude.setText(location.getLatitude() + "");
        longitude.setText(location.getLongitude() + "");
        speed.setText(location.getSpeed()+"");
        area.setText(location.getAddrStr());
        MainHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        LocaInfo locaInfo = realm.createObject(LocaInfo.class);
                        locaInfo.setLongitude(location.getLongitude()+"");
                        locaInfo.setLatitude(location.getLatitude()+"");

                        Toast.makeText(MainActivity.this, "数据插入成功", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


    }

    public  class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {


            if (location != null && (location.getLocType() != BDLocation.TypeServerError &&
                    location.getLocType() != BDLocation.TypeOffLineLocationFail &&
                    location.getLocType() != BDLocation.TypeOffLineLocationNetworkFail)) {

                showLocation(location);

            } else {
                Toast.makeText(MainActivity.this, "定位失败", Toast.LENGTH_SHORT).show();
            }

            //mLocationClient.stop();

        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab:
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                //mWebView.loadUrl("javascript:window.location.href=\"http://www.foodmall.com\"");
                //mWebView.loadUrl("javascript:window.alert('jj')");
                //List<LocaInfo> loca = realm.where(LocaInfo.class).findAll();
                ///LocaInfo info = realm.where(LocaInfo.class).findFirst();
                RealmResults<LocaInfo> results = realm.where(LocaInfo.class).beginsWith("longitude","LocaInfo").findAll();


                break;
            default:
                break;

        }
    }

    public class MyJavaScriptInterface {
        Context mContext;

        MyJavaScriptInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void getLatituAndLongitude() {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLocationClient.start();
                    String jsonStr = "{'lat':'"+latitude.getText()+"','lng':'"+longitude.getText()+"'}";
                    String jsFunc = "javascript:getLocalLatituAndLongitude("+jsonStr+")";
                    //String jsFunc = "javascript:show()";
                    //mWebView.getSettings().setJavaScriptEnabled(true);
                    mWebView.loadUrl(jsFunc);

                    //Toast.makeText(mContext, "JS触发！！", Toast.LENGTH_SHORT).show();
                }
            });

        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        //关闭定位
        mLocationClient.stop();
        realm.close();

    }
}
