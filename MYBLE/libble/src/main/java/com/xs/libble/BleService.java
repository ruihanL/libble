package com.xs.libble;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class BleService extends Service{
    private String TAG = "libble";
    //蓝牙设备相关
    private BluetoothDevice mDevice;
    private BluetoothManager mManager;
    private BluetoothAdapter mAdapter;
    private String mMac = null;
    private List<BluetoothGatt> gatts  = new ArrayList<>();
    private BluetoothGatt currentGatt;

    public  BluetoothGattCharacteristic readChar;   //读取
    public  BluetoothGattCharacteristic writeChar;  //写

    private List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();

    //连接状态定义：未连接--连接中--GATT连接--UUID设置--
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0; //完全未连接
    private static final int STATE_CONNECTING = 1;   //正在连接中
    private static final int STATE_CONNECTED = 2;    //GATT已经连接，但是未有服务
    private static final int STATE_VALID    = 3;     //连接是正确的。

    public final static String ACTION_GATT_CONNECTED           = "com.xs.libble.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED        = "com.xs.libble.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_UNDISCOVERED = "com.xs.libble.ACTION_GATT_SERVICES_UNDISCOVERED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.xs.libble.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE           = "com.xs.libble.ACTION_DATA_AVAILABLE";

    public final static String ACTION_SCAN                      = "com.xs.libble.ACTION_SCAN";
    public final static String ACTION_SCAN_RET                   = "com.xs.libble.ACTION_SCAN_RET";
    public final static String ACTION_SCAN_BEGIN                   = "com.xs.libble.ACTION_SCAN_BEGIN";

    public final static String EXTRA_DATA                       = "com.xs.libble.EXTRA_DATA";

    public final static String ORDER = "ORDER";
    public final static int CMD_START = 0;
    public final static int CMD_STOP = 1;
    public final static int CMD_SCAN = 2;
    public final static int CMD_CONNECT = 3;
    public final static int CMD_DISCONNECT = 4;
    public final static int CMD_RECONNECT = 5;
    public final static int CMD_WRITE = 7;
    public final static int CMD_SCAN_STOP = 8;
    public final static int CMD_DESTROY = 9;


    public Thread watchDeviceStateService = null;//监控设备的开关状态，如蓝牙开关。
    public Boolean watchDeviceStateServiceFlag = true;
    public Boolean watchDeviceStateServiceLastState = false;

    public Thread connectService;
    public Boolean connectServiceFlag = true;
    public Boolean connectServiceReconnectFlag = false;//
    private Handler connectHandler;

    public Thread scanService = null;
    public Boolean scanServiceFlag = true;
    private Handler scanHandler;


    private Handler watchgattHandler;
    private Thread watchgattService = null;
    private Boolean watchgattServiceFlag = true;

    private final int rssi_cnt = 10;
    private int[] rssis = new int[rssi_cnt];//保存最近的rssi_cnt个rssi值
    private int rssi_index = 0;



    public static Boolean RUN_STATE = false; //后台服务状态。

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG,"BleService run onCreate");
        sendNoticeMsg("暂无连接");
        BleService.RUN_STATE = true;

        if (initialize()){
            Log.e("init","success.");

            initHandler();

            openBleAdapter();

            startWatchDeviceStateService();

            startConnectService();

            startWatchGattService();

        }else{
            Log.e("init","fail,now shut down the service.");
            stopSelf();
        }
    }


    private void openBleAdapter()
    {
        watchDeviceStateServiceLastState = mAdapter.isEnabled();
        if (!mAdapter.isEnabled()){
            mAdapter.enable();
            Log.e(TAG,"now try to enable the adapter.");
        }
    }

    private void startConnectService()
    {
        connectService = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG,"the connectService thread is running");
                while (connectServiceFlag){
                    //假如开启了自动重连的机制，就会定时发送指令进行连接检查和操作。
                    if (connectServiceReconnectFlag){
                        if(!isGattConnected())
                        {
                            connect(mMac);
                        }
                    }else {

                    }

                    try {Thread.sleep(5000);}catch (Exception e){}
                }
                Log.e(TAG,"the connectService thread stop");
            }
        });
        connectServiceFlag = true;
        connectService.start();
    }


    /**
     * 启动设备开关检查线程，该线程对蓝牙开关进行检查。
     */
    private void startWatchDeviceStateService()
    {
        watchDeviceStateService = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG,"the watchDeviceStateService thread is running");
                while(watchDeviceStateServiceFlag)
                {
                    Boolean openOrClose = mAdapter.isEnabled();
                    //出现状态变化
                    if (openOrClose!=watchDeviceStateServiceLastState){
                        //打开了开关
                        if (openOrClose)
                        {
                            Log.e(TAG,"you turn on the bluetootch");
                        }else{
                            Log.e(TAG,"you true off the bluetooch");
                        }
                    }else
                    {
                        if (openOrClose)
                        {

                        }else{

                        }
                    }
                    Log.e(TAG,"the bluetootch adapter state is "+openOrClose);
                    watchDeviceStateServiceLastState = openOrClose;

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.e(TAG,"the watchDeviceStateService thread is stop");
            }
        });
        watchDeviceStateServiceFlag = true;
        watchDeviceStateService.start();
    }

    private void startWatchGattService()
    {
        watchgattService = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG,"the watchgattService thread is running");
                while (watchgattServiceFlag){
                    watchgattHandler.sendEmptyMessage(0);
                    try {Thread.sleep(1000);}catch (Exception e){}
                }
                Log.e(TAG,"the watchgattService thread stop");
            }
        });
        watchgattServiceFlag = true;
        watchgattService.start();
    }

    /**
     * 初始化主线程中的Handler
     */
    private void initHandler()
    {
        //处理蓝牙扫描发送过来的信息。
        scanHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Log.e(TAG,"handler:"+msg.what);
                if (msg.what==0){
                    devices.clear();//清空设备数组。
                    broadcastUpdate(ACTION_SCAN_BEGIN);//开始扫描
                    scanLeDevice(true);

                }else if (msg.what==1){
                    scanLeDevice(false);
                    scanLeDevice(true);
                    scanHandler.sendEmptyMessageDelayed(1,1000);

                }else if(msg.what==2){
                    scanHandler.removeMessages(0);
                    scanHandler.removeMessages(1);
                    scanHandler.removeMessages(2);
                    scanLeDevice(false);
                    //把结果广播出去
                    final String[] strings = new String[devices.size()];
                    for (int i = 0;i<devices.size();i++){
                        strings[i] = devices.get(i).getName()+":"+devices.get(i).getAddress();
                    }
                    broadcastUpdate(ACTION_SCAN_RET,strings);

                    devices.clear();//清空设备数组。
                }
                return false;
            }
        });


        watchgattHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                int what = msg.what;
                if (what==0){
                    if (currentGatt!=null)
                    {
                        currentGatt.readRemoteRssi();

                    }

                    //当关掉手机蓝牙设备的时候，此处会有异常
                    try {
                        int state = mManager.getConnectionState(mDevice,BluetoothProfile.GATT);
                        Log.e(TAG,"gatt state:"+state+"");
                    }catch (Exception e){

                    }


                }else{
                    Log.e(TAG, Arrays.toString(rssis));
                    Boolean ret = true;
                    for (int j= 0;j<rssi_cnt;j++){
                        if(rssis[0] != rssis[j]){
                            ret = false;
                        }
                    }

                    //假如rssi_cnt个rssi值是一样的话，就断开连接
                    if (ret){
                        if (currentGatt!=null){
                            currentGatt.disconnect();
                        }
                        rssi_index = 0;
                        for (int w= 0;w<rssi_cnt;w++){
                            rssis[w]=0;
                        }
                    }else{
                        //一样的情况下，重新存储
                        rssi_index = 0;
                        for (int w= 0;w<rssi_cnt;w++){
                            rssis[w]=0;
                        }
                    }
                }

                return false;
            }
        });


    }


    private void destroyThread()
    {
        if (watchDeviceStateService!=null){
            watchDeviceStateServiceFlag = false;
            watchDeviceStateService.interrupt();
        }
        watchDeviceStateService = null;

        if (scanService!=null){
            scanServiceFlag = false;
            scanService.interrupt();
        }
        scanService = null;

        if (connectService!=null){
            connectServiceFlag = false;
            connectService.interrupt();
        }
        connectService = null;

        if (watchgattService!=null){
            watchgattServiceFlag = false;
            watchgattService.interrupt();
        }
        watchgattService = null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (currentGatt!=null)
        {
            currentGatt.disconnect();
            currentGatt.close();
            gatts.remove(currentGatt);
            currentGatt = null;
        }

        for (int i = 0;i<gatts.size();i++)
        {
            gatts.get(i).disconnect();
            gatts.get(i).close();
            gatts.remove(i);
        }

        destroyThread();

        BleService.RUN_STATE = false;
        Log.e(TAG,"ble service run onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent!=null) {
            int i = intent.getIntExtra(ORDER, -1);
            if (i > -1) {
                Log.e("receive the command",i+"");
                switch (i) {
                    case CMD_START://启动服务
                        break;
                    case CMD_STOP://停止服务
                        break;
                    case CMD_SCAN://扫描服务
                        scan(intent.getIntExtra("time",5));
                        break;
                    case CMD_SCAN_STOP:
                        stopScan();
                        break;
                    case CMD_CONNECT://连接指定设备
                        connect(intent.getStringExtra("mac"));
                        break;
                    case CMD_DISCONNECT://断开设备。
                        disconnect();
                        break;
                    case CMD_RECONNECT://重新发起连接
                        //重连操作，都是去读取配置文件中保留的MAC地址，此为连接成功过的设备。
                        String m = getSharedPreferences("mac", 0).getString("mac",null);
                        if (m==null){
                            Log.e(TAG,"do not have the last mac");
                        }else
                        {
                            Log.e("sp","mac is " + m);
                            connect(m);
                        }

                        break;
                    case CMD_WRITE:
                        if (writeChar!=null)
                            writeCharacteristic(intent.getByteArrayExtra("data"),writeChar);
                        else
                            Log.e(TAG,"writeChar is null");
                        break;

                    case CMD_DESTROY:
                        stopSelf();
                        break;
                    default:
                        break;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


    /**
     * 初始化
     *
     * @return 是否成功。
     */
    private boolean initialize() {
        try {
            if (mManager == null) {
                mManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                if (mManager == null) {
                    Log.e(TAG,"the bluetooth manager is null");
                    return false;
                }
            }

            mAdapter = mManager.getAdapter();
            if (mAdapter == null) {
                Log.e(TAG,"the bluetooth adapter is null");
                return false;
            }

            if (!getPackageManager().hasSystemFeature("android.hardware.bluetooth_le"))
            {
                Log.e(TAG,"the system version is not support the ble");
                if (Ble.getmUiContext()!=null){
                    new AlertDialog.Builder(Ble.getmUiContext())
                            .setMessage("对不起，您的手机不支持BLE通讯[请确保系统版本在4.3以上]。")
                            .setPositiveButton("确定", null)
                            .show();
                }
                return false;
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return false;
        }
        return true;
    }


    /**
     *连接GATT服务
     *
     */
    private synchronized boolean connect(final String mac){
        sendNoticeMsg("正在连接");
        Log.e(TAG,"try to connect:"+mac);
        if (mAdapter == null || mac == null||!BluetoothAdapter.checkBluetoothAddress(mac)) {
            Log.e(TAG,"the mac is invalid.");
            return false;
        }
        //假如mac是新的，就重新创建一个远程设备对象。
        if (mMac!=null&&mac.equals(mMac)){
            if (mDevice == null)
            {
                mDevice = mAdapter.getRemoteDevice(mac);
            }
        }else{
            mDevice = null;
            mDevice = mAdapter.getRemoteDevice(mac);
        }

        //此时检查是否创建远程设备成功。
        if (mDevice == null) {
            Log.e("connect","can't make the device");
            return false;
        }

        if (gatts.size()>0)
        {
            for (BluetoothGatt g:gatts)
            {
                g.disconnect();
                g.close();
                gatts.remove(g);
            }
        }

        BluetoothGatt t = mDevice.connectGatt(this,false,makeNewCallback());
        currentGatt = t;
        gatts.add(t);
        Log.e(TAG,"make a new gatt:"+currentGatt.toString()+" gatts size is:"+gatts.size());

        mMac = mac;
        return true;
    }

    private BluetoothGattCallback makeNewCallback()
    {
        BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onDescriptorRead(BluetoothGatt gatt,
                                         BluetoothGattDescriptor descriptor, int status) {
                // TODO Auto-generated method stub
                super.onDescriptorRead(gatt, descriptor, status);

            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt,
                                          BluetoothGattDescriptor descriptor, int status) {
                // TODO Auto-generated method stub
                super.onDescriptorWrite(gatt, descriptor, status);

            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                // TODO Auto-generated method stub
                super.onReadRemoteRssi(gatt, rssi, status);
                Log.e(TAG,"onReadRemoteRssi:"+status+" "+rssi);
                sendRSSI(rssi);


                if (rssi_index>rssi_cnt-1){
                    watchgattHandler.sendEmptyMessage(1);
                }else{
                    rssis[rssi_index] = rssi;
                    rssi_index++;
                }
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                // TODO Auto-generated method stub
                super.onReliableWriteCompleted(gatt, status);

            }


            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.e(TAG,"connect state:"+newState+":"+gatt.toString());

                if (newState == BluetoothProfile.STATE_CONNECTED) {

                    mConnectionState = STATE_CONNECTED;
                    broadcastUpdate(ACTION_GATT_CONNECTED);

                    gatt.discoverServices();
                    sendNoticeMsg("正在匹配");
                    connectServiceReconnectFlag = true;
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                    mConnectionState = STATE_DISCONNECTED;
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);

                    readChar = null;
                    writeChar = null;

                    //收到断开信号后，关闭GATT，回收资源。
                    gatt.close();
                    gatts.remove(gatt);
                    gatt = null;
                    currentGatt = null;
                    sendNoticeMsg("暂无连接");
                }


            }


            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                //服务查找失败，应该多次进行查找。
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    readChar = null;
                    writeChar = null;
                    Boolean ok = findService(gatt,getSupportedGattServices(gatt));

                    //查找UUID，假如UUID符合，则确定为成功，若UUID不符合，认为是连接失败。
                    if (ok) {
                        mConnectionState = STATE_VALID;
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                        //将连接成功的MAC写入到配置中
                        getSharedPreferences("mac",0).edit().putString("mac",mMac).commit();
                        connectServiceReconnectFlag = true;

                        currentGatt = gatt;//当前有效的GATT。

                        sendNoticeMsg("已连接设备:"+mMac);
                    }else{
                        mConnectionState = STATE_DISCONNECTED;
                        broadcastUpdate(ACTION_GATT_SERVICES_UNDISCOVERED);

                        connectServiceReconnectFlag = false;
                        currentGatt.disconnect();
                        currentGatt.close();
                        gatts.remove(currentGatt);
                        currentGatt = null;

                        Toast.makeText(BleService.this,"该设备UUID不匹配，已停止连接。",Toast.LENGTH_LONG).show();
                        sendNoticeMsg("暂无连接");
                        //stopSelf();
                    }

                } else {
                    mConnectionState = STATE_DISCONNECTED;
                    //恢复状态为未连接,系统定时器重新进行连接gatt。
                }

            }


            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic,
                                             int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }
            }


            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic) {

                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt,
                                              BluetoothGattCharacteristic characteristic, int status) {
                // TODO Auto-generated method stub
                super.onCharacteristicWrite(gatt, characteristic, status);
            }
        };

        return bluetoothGattCallback;
    }




    /**
     *  发送广播
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * 需要把数据也传递
     * @param action
     * @param c
     */
    private void broadcastUpdate(final String action,final BluetoothGattCharacteristic c)
    {
        final Intent intent = new Intent(action);
        if (Dev.NOTIFY.equals(c.getUuid())) {
            final byte[] data = c.getValue();
            if (data != null && data.length > 0) {
                intent.putExtra(EXTRA_DATA, data);
            }
        }
        sendBroadcast(intent);
    }

    /**
     * 广播单个Mac地址
     * @param action
     * @param macs
     */
    private void broadcastUpdate(final String action,String macs)
    {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, macs);
        sendBroadcast(intent);
    }

    /**
     * 广播扫描结果。
     * @param action
     * @param macs
     */
    private void broadcastUpdate(final String action,String macs[])
    {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, macs);
        sendBroadcast(intent);
    }

    /**
     * 获取GATT服务
     * @return
     */
    private List<BluetoothGattService> getSupportedGattServices(BluetoothGatt gatt) {
        if (gatt == null) return null;
        return gatt.getServices();
    }

    /**
     * 校验连接UUID
     * @param paramList
     */
    private Boolean findService(BluetoothGatt gatt,List<BluetoothGattService> paramList)
    {
        readChar = null;
        writeChar = null;
        for (BluetoothGattService mService:paramList) {
            if (mService.getUuid().toString().equalsIgnoreCase(Dev.SERVICE.toString())) {
                List<BluetoothGattCharacteristic> localList = mService.getCharacteristics();
                for (BluetoothGattCharacteristic mChar:localList) {
                    if (mChar.getUuid().toString().equalsIgnoreCase(Dev.NOTIFY.toString())) {
                        readChar = mChar;
                        setCharacteristicNotification(gatt, mChar, true);
                    }
                    if (mChar.getUuid().toString().equalsIgnoreCase(Dev.WRITE.toString())) {
                        writeChar = mChar;
                    }
                }
            }
        }
        if (readChar!=null&&writeChar!=null) {

            return true;
        }
        return false;
    }

    private void setCharacteristicNotification(BluetoothGatt gatt,BluetoothGattCharacteristic c,boolean enabled) {
        if (mAdapter == null || gatt == null) {
            return;
        }
        gatt.setCharacteristicNotification(c, enabled);

        if (Dev.needConfig) {
            if (Dev.NOTIFY.equals(c.getUuid())) {
                BluetoothGattDescriptor d = c.getDescriptor(Dev.CONFIG);

                d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(d);
            }
        }
    }

    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mAdapter == null || currentGatt == null) {
            return;
        }
        currentGatt.readCharacteristic(characteristic);
    }

    private  void writeCharacteristic(byte[] msg,BluetoothGattCharacteristic c)
    {
        if (currentGatt == null) {
            return;
        }
        if (c==null)return;
        c.setValue(msg);//假如是字符串，可以
        currentGatt.writeCharacteristic(c);
    }
    private  void writeCharacteristic(String msg,BluetoothGattCharacteristic c)
    {
        if (currentGatt == null) {
            return;
        }
        if (c==null)return;
        c.setValue(msg);//假如是字符串，可以
        currentGatt.writeCharacteristic(c);
    }


    private void disconnect() {
        if (mAdapter == null || currentGatt == null) {
            return;
        }
        currentGatt.disconnect();
        //用户主动断开连接的，取消自动重连。
        connectServiceReconnectFlag = false;

    }


    /**
     * 启动周期性扫描，利用消息
     * @param time
     */
    private void scan(int time)
    {
        scanHandler.sendEmptyMessageDelayed(0,0);
        scanHandler.sendEmptyMessageDelayed(1,1000);
        scanHandler.sendEmptyMessageDelayed(2,time*1000);//发送停止命令
    }

    private void stopScan()
    {
        scanHandler.sendEmptyMessage(2);
    }

    /**
     * 扫描周边的人。不能长时间扫描
     * @param enable
     */
    private  void scanLeDevice(boolean enable) {
        if (enable) {
            mAdapter.startLeScan(mLeScanCallback);
        } else {
            mAdapter.stopLeScan(mLeScanCallback);
        }
    }


    BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(BluetoothDevice arg0, int arg1, byte[] arg2) {
            // TODO Auto-generated method stub
            if (arg0.getAddress()==null) {
                return;
            }

            if (devices.size()>30) {
                return;
            }else {
                if (!devices.contains(arg0)) {
                    devices.add(arg0);
                    broadcastUpdate(BleService.ACTION_SCAN,arg0.getName()+":"+arg0.getAddress());
                    Log.e(TAG,arg0.getName()+":"+arg0.getAddress());
                }
            }
        }
    };

    private Boolean isGattConnected()
    {
        if (mManager!=null&&mDevice!=null&&mAdapter!=null){
            if (!mAdapter.isEnabled())
                return false;
            int state = mManager.getConnectionState(mDevice,BluetoothProfile.GATT);
            Log.e("the connection state:",state+"");
            if (state==2)
                return true;
            else
                return false;
        }else
            return false;
    }


    String noticeMsg;
    private void sendNoticeMsg(String msg)
    {

        Notification noti = new Notification.Builder(BleService.this)
                .setContentTitle("BLE连接")
                .setContentText(msg)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
        startForeground(4836, noti);

        noticeMsg = msg;
    }

    private void sendRSSI(int rssi)
    {
        Notification noti = new Notification.Builder(BleService.this)
                .setContentTitle("BLE连接")
                .setContentText(noticeMsg+"["+rssi+"DB]")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
        startForeground(4836, noti);

    }
}
