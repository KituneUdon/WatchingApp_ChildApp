package com.example.s162132.childapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.skyway.Peer.DataConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;
import io.skyway.Peer.PeerError;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;

public class MainActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks,
                    com.google.android.gms.location.LocationListener,
                    SkyWayIdSetting.successfulGetPeerIdListener{

    private BluetoothAdapter mBluetoothAdapter;
    final private int REQUEST_ENABLE_BT = 1;
    private Peer peer;
    private String myId, parentId;
    private DataConnection dataConnection;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothServerSocket mBluetoothServerSocket;
    private final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String NAME = "BluetoothTest";
    private LocationManager mLocationManager = null;
    private final String RECEIVED_GPS = "Tell me the location";
    private final String RECEIVED_BLUETOOTH_CONNECT = "Connect with Bluetooth";
    private final String RECEIVED_BLUETOOTH_DISCONNECT = "Disconnect with Bluetooth";
    private final String CONNECT_WITH_WiFi = "connect with wifi";
    private final String DISCONNECT_FROM_WiFi = "disconnect from wifi";
    private final String HOME_CONFIRMATION = "home confirmation";
    private SkyWayIdSetting skyWayIdSetting = new SkyWayIdSetting();
    private Handler handler = new Handler();
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private WifiConnectionWatcher wifiConnectionWatcher = new WifiConnectionWatcher();
    private SharedPreferences pref1;
    private boolean wifiConnectingFlg;
    private boolean cantWifiConnectionFlg = false;
    private boolean calledBroadcastreceiverFlg = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        skyWayIdSetting.successfulGetPeerId(this);
        pref1 = getSharedPreferences("SkyWayId", Context.MODE_PRIVATE);

        //https://firespeed.org/diary.php?diary=kenz-1821
        //パーミッションの許可を求める
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            // 許可されている時の処理
        } else {
            //許可されていない時の処理
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CALL_PHONE)) {
                //拒否された時 Permissionが必要な理由を表示して再度許可を求めたり、機能を無効にしたりします。
            } else {
                //まだ許可を求める前の時、許可を求めるダイアログを表示します。
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, 0);
            }
        }

        googleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();

        //bluetoothを確認する
        bluetoothCheck();

        findViewById(R.id.CallList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContactInformation();
            }
        });

        findViewById(R.id.CallButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callParent();
            }
        });

        findViewById(R.id.WifiButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, WiFiSettingActivity.class);
                startActivity(intent);
            }
        });
        BootstrapButton button = (BootstrapButton) findViewById(R.id.CallButton);
    }

    public void mainProcessing() {
        //skyWayのidを取得する
        final Context context = getApplicationContext();
        peer = skyWayIdSetting.getPeerId(context);

        setPeerCallBack();
        System.out.println("called:mainProcessing:setPeerCallback");

        //GPSサービス取得
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        checkLocationPreference();

        //bluetoothの通信を別スレッドで開始する。
        //通信のところは本当は別クラスで実装したがったができなかったためMainActivityに書いてある
        exchangeSkyId();

        //ビデオをセットするための準備をする。
        VideoSetting videoSetting = new VideoSetting(peer);
        videoSetting.getStream();

        //ブロードキャストレシーバーの準備
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION); // "android.net.wifi.STATE_CHANGE"
        registerReceiver(wifiConnectionWatcher, intentFilter);
    }

    public void bluetoothCheck() {
        //BluetoothがONになっているかどうか
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //mBluetoothAdapterがnullだった場合は端末がbluetoothをサポートしてない。
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "この端末はbluetoothに対応していません。"
                    , Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            //bluetoothがOFFになっているときの処理
            //bluetoothを有効にするかどうかの確認の画面を呼び出す
            Intent reqEnableBTIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(reqEnableBTIntent, REQUEST_ENABLE_BT);
        } else {
            //ONになっているときの処理
            //Toast.makeText(this, "ONになっています。", Toast.LENGTH_SHORT).show();
            //処理を実行する
            mainProcessing();
        }
    }

    public void exchangeSkyId() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mBluetoothServerSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
                    while (true) {
                        mBluetoothSocket = mBluetoothServerSocket.accept();
                        if (mBluetoothSocket != null) {
                            OutputStream out = mBluetoothSocket.getOutputStream();

                            SharedPreferences pref = getSharedPreferences("SkyWayId", MODE_PRIVATE);
                            myId = pref.getString("myId", null);

                            System.out.println("MainActivity:myId=" + myId);

                            ObjectOutputStream outObj = new ObjectOutputStream(out);
                            out.flush();
                            outObj.writeObject(myId);
                            out.flush();
                            mBluetoothServerSocket.close();
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        mBluetoothSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    //bluetoothのダイアログでボタンを押された時に実行される。
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    //なぜか実行される
                    mainProcessing();
                } else {
                    Toast.makeText(this, "Bluetoothを許可しないとこのアプリは使用できません", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    public void sendData() {
        System.out.println("sendData:mLocationManager = " + mLocationManager);

        Location location = getLocation();
        if (location == null) {
            System.out.println("位置情報の取得に失敗しています。");
            return;
        } else {
            String data = location.getLatitude() + "," + location.getLongitude();
            System.out.println("sendData:dataConnection = " + dataConnection);
            boolean bResult = dataConnection.send(data);
            if (bResult) {
                System.out.println("送信成功");
            } else {
                System.out.println("送信失敗");
            }
        }
    }

    public void checkLocationPreference() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        final PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                final LocationSettingsStates states = locationSettingsResult.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, 1000);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                            e.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    public Location getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        System.out.println("MainActivity:googleApiClient = " + googleApiClient);
        Location lastLocation = FusedLocationApi.getLastLocation(googleApiClient);

        System.out.println("MainActivity:getLocation = " + lastLocation);

        return lastLocation;
    }


    public void setPeerCallBack() {
        peer.on(Peer.PeerEventEnum.CONNECTION, new OnCallback() {
            public void onCallback(Object object) {
                //接続イベント受信時の処理
                if (object instanceof DataConnection) {
                    dataConnection = (DataConnection) object;
                    parentId = dataConnection.peer;
                    System.out.println("PeerEventEnum.CONNECTION:parentId = " + parentId);
                    SharedPreferences.Editor editor = pref1.edit();
                    editor.putString("parentId", parentId);
                    editor.commit();
                    System.out.println("PeerEventEnum.CONNECTION:dataConnection=" + dataConnection);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "親機と接続しました。", Toast.LENGTH_SHORT).show();
                        }
                    });
                    setDataConnectionCallBack();
                }
            }
        });

        peer.on(Peer.PeerEventEnum.DISCONNECTED, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                System.out.println("called:PeerEventEnum.DISCONNECTED");
                peer.destroy();
                Context context = getApplicationContext();
                peer = skyWayIdSetting.getPeerId(context);
            }
        });

        peer.on(Peer.PeerEventEnum.CLOSE, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                System.out.println("called:PeerEventEnum.CLOSE");
            }
        });

        peer.on(Peer.PeerEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                System.out.println("peerに重大なエラーが発生しました。");
                PeerError peerError = (PeerError) o;
                System.out.println("PeerEventEnum.ERROR:peerError = " + peerError.type);
                System.out.println("PeerEventEnum.ERROR:peerError.message = " + peerError.message);
                System.out.println("PeerEventEnum.ERROR:peerError.exception = " + peerError.exception);

                if (peerError.message.equals("Retrieve Peer Id is failed")) {
                    Context context = getApplicationContext();
                    skyWayIdSetting.getPeerId(context);
                }
            }
        });
    }

    public void setDataConnectionCallBack() {
        //受信したときの処理
        dataConnection.on(DataConnection.DataEventEnum.DATA, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                System.out.println("called:DataEventEnum.DATA");
                System.out.println("Object = " + o);
                if (o instanceof String) {
                    String str = (String) o;
                    if (str.equals(RECEIVED_GPS)) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                //位置情報を送信する。
                                sendData();
                            }
                        });
                    } else if (str.equals(RECEIVED_BLUETOOTH_CONNECT)) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("迷子防止機能を実行するためのフラグを受け取った。");
                                try {
                                    if (mBluetoothSocket != null) {
                                        mBluetoothSocket.close();
                                        mBluetoothSocket = null;
                                    }
                                    mBluetoothServerSocket = mBluetoothAdapter.
                                            listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
                                    while (true) {
                                        mBluetoothSocket = mBluetoothServerSocket.accept();
                                        if (mBluetoothSocket != null) {
                                            //接続に成功
                                            break;
                                        }
                                    }
                                } catch (IOException e) {
                                    try {
                                        mBluetoothSocket.close();
                                        e.printStackTrace();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            }
                        }).start();
                    } else if (str.equals(RECEIVED_BLUETOOTH_DISCONNECT)) {
                        try {
                            mBluetoothSocket.close();
                            mBluetoothSocket = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (str.equals(HOME_CONFIRMATION)) {
                        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                        String s = info.getTypeName();
                        boolean b = false;
                        if (s.equals("MOBILE")) {
                            b = dataConnection.send(DISCONNECT_FROM_WiFi);
                            //wifiConnectingFlgがfalseの時は外出中
                            wifiConnectingFlg = false;
                        } else if (s.equals("WIFI")) {
                            b = dataConnection.send(CONNECT_WITH_WiFi);
                            //wifiConnectingFlgがfalseの時は在宅してる
                            wifiConnectingFlg = true;
                        }
                        if (b) {
                            System.out.println("起動時の在宅確認の送信成功");
                        } else {
                            System.out.println("起動時の在宅確認の送信失敗");
                        }
                    }
                }
            }
        });

        dataConnection.on(DataConnection.DataEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                System.out.println("DataConnectionにエラーが発生しました。");
            }
        });

        dataConnection.on(DataConnection.DataEventEnum.CLOSE, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                System.out.println("called:DataEventEnum.CLOSE");
                //親端末が接続を切断したとき
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "親機との接続が切断されました。", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    public void callParent() {
        String s = pref1.getString("Key", "");

        Uri uri = Uri.parse("tel:" + s);
        Intent i = new Intent(Intent.ACTION_CALL, uri);
        startActivity(i);
    }

    public void setContactInformation() {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.dialog_contents,
                (ViewGroup) findViewById(R.id.layout_root));

        //アロートダイアログを生成
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("連絡先");
        builder.setView(layout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                //OKボタンのクリック処理
                EditText text
                        = (EditText) layout.findViewById(R.id.edit_text);

                //共有プリファレンス処理
                String s = text.getText().toString();
                SharedPreferences.Editor e = pref1.edit();
                e.putString("Key", s);
                e.commit();
                //共有プリファレンス処理
            }

        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                //Cancelのボタン処理
            }
        });
        //表示
        builder.create().show();
    }

    @Override
    public void successfulGetPeerId() {
        System.out.println("called:successfulGetPeerId");
        if (!wifiConnectingFlg) {
            if (parentId != null) {
                System.out.println("親との接続開始");
                dataConnection = skyWayIdSetting.connectStart(parentId, peer);
                calledBroadcastreceiverFlg = true;
            }
        }
    }

    public class WifiConnectionWatcher extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (dataConnection != null) {
                if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) { // "android.net.wifi.STATE_CHANGE"
                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    switch (info.getState()) {
                        case DISCONNECTED:
                            if (wifiConnectingFlg == true && searchSSID()) {
                                System.out.println("called:wifiDisconnected");
                                Toast.makeText(MainActivity.this, "wifiから切断しました。", Toast.LENGTH_SHORT).show();
                                sentFlg(DISCONNECT_FROM_WiFi);
                                cantWifiConnectionFlg = true;
                                wifiConnectingFlg = false;
                                calledBroadcastreceiverFlg = true;
                            }
                            break;
                        case SUSPENDED:
                            break;
                        case CONNECTING:
                            break;
                        case CONNECTED:
                            if (wifiConnectingFlg == false && searchSSID()) {
                                System.out.println("called:wifiConnected");
                                Toast.makeText(MainActivity.this, "wifiと接続しました。", Toast.LENGTH_SHORT).show();
                                sentFlg(CONNECT_WITH_WiFi);
                                wifiConnectingFlg = true;
                            }
                            break;
                        case DISCONNECTING:
                            break;
                        case UNKNOWN:
                            break;
                        default:
                    }
                }
            }
        }

        public boolean searchSSID() {
            SharedPreferences preferences = getSharedPreferences("SSID", MODE_PRIVATE);
            String str = preferences.getString("SSID", null);
            String[] value = str.split(",", 0);
            if (str != null) {
                WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
                if (manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                    List<ScanResult> results = manager.getScanResults();
                    for (int i=0;i<results.size();++i) {
                        String ssid = results.get(i).SSID;
                        if (Arrays.asList(value).contains(ssid)) {
                            return true;
                        }
                    }
                }
            } else {
                Toast.makeText(MainActivity.this, "WiFiが保存されていません。", Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        public void sentFlg(String flg) {
            System.out.println("called:sentFlg");
            boolean bool = dataConnection.send(flg);
            if (bool) {
                //送信に成功した
                System.out.println("Wifi状態変化フラグの送信成功");
            } else {
                //送信に失敗した。
                Toast.makeText(MainActivity.this, "親機と接続が切断されている可能性があります。", Toast.LENGTH_SHORT).show();
                System.out.println("Wifi状態変化フラグの送信失敗");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("called:onDestroy");
        if (dataConnection != null) {
            dataConnection.close();
        }
        if (peer != null) {
            peer.disconnect();
            peer.destroy();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        googleApiClient.connect();
        System.out.println("called:onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    //google play serviceと接続したときに実行される
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //Location lastLocation = FusedLocationApi.getLastLocation(googleApiClient);
        //if (lastLocation == null) { return; }
        //Log.v("Location", "lat=" + lastLocation.getLatitude() + " lon=" + lastLocation.getLongitude());
        FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest,  this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }
}