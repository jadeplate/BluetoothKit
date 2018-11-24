package benefit.platfrom.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.GridLayoutManager;
import android.util.EventLog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.blakequ.bluetooth_manager_lib.BleManager;
import com.blakequ.bluetooth_manager_lib.BleParamsOptions;
import com.blakequ.bluetooth_manager_lib.connect.BluetoothSubScribeData;
import com.blakequ.bluetooth_manager_lib.connect.ConnectConfig;
import com.blakequ.bluetooth_manager_lib.connect.ConnectState;
import com.blakequ.bluetooth_manager_lib.connect.ConnectStateListener;
import com.blakequ.bluetooth_manager_lib.connect.multiple.MultiConnectManager;
import com.blakequ.bluetooth_manager_lib.scan.BluetoothScanManager;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanCallbackCompat;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanResultCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.sql.Array;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSION = 1;// 权限请求码  用于回调

    private MultiConnectManager multiConnectManager; //多设备连接
    private BluetoothAdapter bluetoothAdapter = null;  //蓝牙适配器


    private ArrayList<String> connectDeviceMacList = null;//需要连接的mac设备集合
    private ArrayList<BluetoothGatt> gattArrayList;  //设备gatt集合

    private BluetoothScanManager scanManager;
    private BlueToothAdapter mAdapter = null;
    private RecyclerView mGridView;
    private TextView btnSelectDevice;

    private ArrayList<String> deviceMacs  = null;// 数据源 ： 所有扫描到的设备mac地址

    private ArrayList<String> selectDeviceMacs = null;// 选择的需要连接的设备的mac集合


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        requestWritePermission();
        mGridView =findViewById(R.id.mGridView);
        btnSelectDevice =findViewById(R.id.btnSelectDevice);
        btnSelectDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanManager.startScanNow(); //立即开始扫描
            }
        });

        mGridView.setLayoutManager( new GridLayoutManager(this, 1));
        mGridView.setAdapter(mAdapter);
        initVariables();
        initConfig();  // 蓝牙初始设置
        initBle();

        scanManager.startScanNow(); //立即开始扫描
    }

    private void initVariables() {
        connectDeviceMacList = new ArrayList<>();
        gattArrayList = new ArrayList<>();
        deviceMacs = new ArrayList<>();
    }

    /**
     * 连接需要连接的传感器
     * @param
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void connentBluetooth() {
        String[] objects = connectDeviceMacList.toArray(new String[connectDeviceMacList.size()]);
        multiConnectManager.addDeviceToQueue(objects);
        multiConnectManager.addConnectStateListener(new ConnectStateListener() {
            @Override
            public void onConnectStateChanged(String address, ConnectState state) {
                if (state.equals(ConnectState.CONNECTING)){
                    Log.i("connectStateX", "设备:" + address + "连接状态:" + "正在连接");
                }else if (state.equals(ConnectState.CONNECTED)){
                    Log.i("connectStateX", "设备:" + address + "连接状态:" + "成功");
                }else if (state.equals(ConnectState.NORMAL)){
                    Log.i("connectStateX", "设备:" + address + "连接状态:" + "连接失败");
                }
            }
        });


        /**
         * 数据回调
         */
        multiConnectManager.setBluetoothGattCallback(new BluetoothGattCallback() {
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                dealCallDatas(gatt, characteristic);
            }


        });


        multiConnectManager.setServiceUUID("0000ffe0-0000-1000-8000-00805f9b34fb");
        multiConnectManager.addBluetoothSubscribeData(
                new BluetoothSubScribeData.Builder().setCharacteristicNotify(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")).build());

        //还有读写descriptor
        //start descriptor(注意，在使用时当回调onServicesDiscovered成功时会自动调用该方法，所以只需要在连接之前完成1,3步即可)
        for (int i = 0; i < gattArrayList.size(); i++) {
            multiConnectManager.startSubscribe(gattArrayList.get(i));
        }

        multiConnectManager.startConnect();

    }

    /**
     * 处理回调的数据
     * @param gatt
     * @param characteristic
     */


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void dealCallDatas(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic ) {
//        int position = connectDeviceMacList.indexOf(gatt.getDevice().getAddress());
        //第一个传感器数据
        byte[] value = characteristic.getValue();
        String str = new String(value);
        Log.d("ssssssssssssssssssssss",str);



    }

    /**
     * 对蓝牙的初始化操作
     */
    private void initConfig() {
        multiConnectManager = BleManager.getMultiConnectManager(this);
        // 获取蓝牙适配器

        try {
            // 获取蓝牙适配器
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            //
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
                return;
            }

            // 蓝牙没打开的时候打开蓝牙
            if (!bluetoothAdapter.isEnabled()){
                bluetoothAdapter.enable();
            }
        } catch (Exception err) {

        }

        BleManager.setBleParamsOptions(new BleParamsOptions.Builder()
                .setBackgroundBetweenScanPeriod((5 * 60 * 1000))
                .setBackgroundScanPeriod(10000)
                .setForegroundBetweenScanPeriod(2000)
                .setForegroundScanPeriod(10000)
                .setDebugMode(BuildConfig.DEBUG)
                .setMaxConnectDeviceNum(7)            //最大可以连接的蓝牙设备个数
                .setReconnectBaseSpaceTime(1000)
                .setReconnectMaxTimes(Integer.MAX_VALUE)
                .setReconnectStrategy(ConnectConfig.RECONNECT_LINE_EXPONENT)
                .setReconnectedLineToExponentTimes(5)
                .setConnectTimeOutTimes(20000)
                .build());
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        if (data != null) {
            if (resultCode == 1){
                connectDeviceMacList = data.getStringArrayListExtra("data");
                Log.i("xqxinfo", "需要连接的mac" + connectDeviceMacList.toString());
                //获取设备gatt对象
                for (int i = 0; i < connectDeviceMacList.size(); i++) {
                    BluetoothGatt gatt = bluetoothAdapter.getRemoteDevice(connectDeviceMacList.get(i)).connectGatt(this, false, new BluetoothGattCallback() {
                        @Override
                        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
                        }
                    });

                    gattArrayList.add(gatt);
                    Log.i("xqxinfo", "添加了" + connectDeviceMacList.get(i));
                }

            }

        }
    }
    /**
     * 初始化蓝牙相关配置
     */
    private void initBle() {
        scanManager = BleManager.getScanManager(this);

//        scanManager.setScanOverListener { }

        scanManager.setScanCallbackCompat(new ScanCallbackCompat() {
            @Override
            public void onScanResult(int callbackType, ScanResultCompat result) {
                super.onScanResult(callbackType, result);
                //scan result
                // 只有当前列表中没有该mac地址的时候 添加
                if (!deviceMacs.contains(result.getDevice().getName() + "--" + result.getDevice().getAddress())) {
                    deviceMacs.add(result.getDevice().getName() + "--" + result.getDevice().getAddress());
                    selectDeviceMacs = new ArrayList<>();
                    selectDeviceMacs.add(result.getDevice().getName() + "--" + result.getDevice().getAddress());
                    mAdapter = new BlueToothAdapter(MainActivity.this,selectDeviceMacs);
                    mGridView.setAdapter(mAdapter);


                }
            }
        });


    }
    /**
     * @author xqx
     * @email djlxqx@163.com
     * blog:http://www.cnblogs.com/xqxacm/
     * createAt 2017/8/30
     * description:  权限申请相关，适配6.0+机型 ，蓝牙，文件，位置 权限
     */

    private ArrayList<String> allPermissionList = new  ArrayList<>();


    /**
     * 遍历出需要获取的权限
     */
    private void requestWritePermission() {
        allPermissionList.add(Manifest.permission.BLUETOOTH);
        allPermissionList.add(Manifest.permission.BLUETOOTH_ADMIN);
        allPermissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        allPermissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        allPermissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        allPermissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        ArrayList<String> permissionList =new ArrayList<>();
        // 将需要获取的权限加入到集合中  ，根据集合数量判断 需不需要添加
        for (int i = 0; i < allPermissionList.size(); i++) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, allPermissionList.get(i))) {
                permissionList.add(allPermissionList.get(i));
            }
        }


        String[] permissionArray =new String[ permissionList.size()];
        for (int i = 0; i < permissionList.size(); i++) {
            permissionArray[i] = permissionList.get(i);
        }

        if (permissionList.size() > 0)
            ActivityCompat.requestPermissions(this, permissionArray, REQUEST_CODE_PERMISSION);
    }

    /**
     * 权限申请的回调
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (permissions[0].equals( Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户同意使用write

            } else {
                //用户不同意，自行处理即可
//                Toast.makeText(this, "您取消了权限申请,可能会影响软件的使用,如有问题请退出重试", Toast.LENGTH_SHORT).show();
            }
        }
    }



    @Subscribe(threadMode = ThreadMode.MAIN) //在ui线程执行
    public void onDataSynEvent(MessageEvent event) {
        if (event.getCode().equals("连接蓝牙")){
            connectDeviceMacList.add(event.getMessage());
            connentBluetooth();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (scanManager.isScanning()) {
            scanManager.stopCycleScan();
        }
    }



}
