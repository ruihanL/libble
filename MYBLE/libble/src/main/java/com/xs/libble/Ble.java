package com.xs.libble;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Ble {
    private static String TAG = "libble";

    private static Ble instance;
    private static Context mContext;   //current Context;
    private static Context mAppContext;//Application Context;
    private static Context mUiContext; //UI context;

    private  Boolean showScanDialog;//是否显示扫描的结果
    private  ProgressDialog scanProgressDialog;//等待对话框
    private Dialog scanRetDialog;//扫描的结果列表

    private Thread watchService; //后台监控线程，可以获知后台服务的情况。
    private Boolean watchServiceFlag = true;//

    private  List<BleListener> bleListeners = new ArrayList<>();

    public interface BleListener{
        void onBleConnect();
        void onBleDisconnect();
        void onBleConnectFail();
        void onBleRead(byte[] data);

        void onBleScanBegin();//开始扫描
        void onBleScan(String name,String mac);//扫描过程中的MAC地址
        void onBleScanRet(String[] names,String[] macs);//扫描结果
    }

    /**
     * 添加回调监听器
     * @param listener
     */
    public void setOnBleListener(BleListener listener)
    {
        if (!bleListeners.contains(listener))
            bleListeners.add(listener);
    }

    /**
     * 移除监听器
     * @param listener
     */
    public void removeBleListener(BleListener listener)
    {
        if (bleListeners.contains(listener))
            bleListeners.remove(listener);
    }




    /**
     * 初始化BLE设备。
     * @param context
     */
    private Ble(Context context){
        //获取上下文
        if (context.equals(context.getApplicationContext())) {
            mAppContext = context;
        }
        else {
            mUiContext = context;
            mAppContext = mUiContext.getApplicationContext();
        }
        mContext = context;
        Log.e(TAG,"the context is[app/ui/current]:"+mAppContext.toString()+" "+ mUiContext.toString()+" "+mContext.toString());

        //注册广播
        mAppContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        Log.e(TAG,"register a receiver to recevie the gatt update msg.");

        init();
    }

    /**
     * 初始化BLE后台服务
     */
    private void init()
    {
        startBleService();
        startWatchService();
    }

    /**
     * 初始化
     * @param autoConnectLastDevices 是否在后台服务启动后，自动搜索连接上次的设备。默认是否
     */
    private void init(Boolean autoConnectLastDevices)
    {
        //BleService. = autoConnectLastDevices;
        Log.e(TAG,"you set the auto_connect_last_devices to "+autoConnectLastDevices.toString());

        init();
    }

    /**
     * 启动监控后台服务状态线程。
     * 此线程反映后台服务的情况。
     */
    private void startWatchService()
    {
        watchService = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG,"the service watching thread is running");
                while(watchServiceFlag)
                {
                    Log.e(TAG,"the bleservice is running?"+BleService.RUN_STATE);

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.e(TAG,"the service watching thread  is stop");
            }
        });
        watchServiceFlag = true;
        watchService.start();
    }

    /**
     * 启动BLE后台服务。
     */
    private void startBleService()
    {
        Bundle bundle = new Bundle();
        bundle.putInt(BleService.ORDER,BleService.CMD_START);
        sendCMD(bundle);
        Log.e(TAG,"send the intent to start the ble service");
    }

    /**
     * 向后台服务发送指令。
     * @param bundle 调用BleService.xxx中的预设指令。
     */
    private void sendCMD(Bundle bundle)
    {
        Intent intent = new Intent(mContext, BleService.class);
        intent.putExtras(bundle);
        mContext.startService(intent);
    }


    /**
     * 双重校验，返回单例
     * 在进行各种操作之前，需要调用此函数。
     * @param context
     * @return
     */
    public static Ble getInstance(Context context)
    {
        if (instance == null){
            synchronized (Ble.class){
                if (instance == null){
                    instance = new Ble(context);
                    Log.e(TAG,"make a new ble instance");
                }
            }
        }
        return instance;
    }

    public static Ble getInstance()
    {
        if (instance == null){
            Log.e(TAG,"the instance is null,you must set the context");

        }
        return instance;
    }

    public static Context getmUiContext() {
        return mUiContext;
    }

    /**
     * 打开手机蓝牙硬件开关
     * @param context
     * @param ask 是否询问用户
     */
    public void openBluetooth(Context context,Boolean ask)
    {
        final BluetoothAdapter mBluetoothAdapter = ((BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (mBluetoothAdapter.isEnabled())
            return;
        if (ask)
        {
            AlertDialog.Builder ab = new AlertDialog.Builder(context);//此处若使用ApplicationContext会有异常
            ab.setTitle("注意");
            ab.setMessage("是否打开蓝牙开关？");
            ab.setPositiveButton("打开", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mBluetoothAdapter.enable();
                }
            });
            ab.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            ab.create().show();
        }else{
            mBluetoothAdapter.enable();
        }

    }

    /**
     * 关闭蓝牙开关
     */
    public void closeBluetooth()
    {
        BluetoothAdapter mBluetoothAdapter = ((BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.disable();
    }

    /**
     * 检查蓝牙开关是否是打开状态
     * @return 是否打开
     */
    public Boolean isBluetoothOpen()
    {
        BluetoothAdapter mBluetoothAdapter = ((BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (mBluetoothAdapter.isEnabled())
            return true;
        return  false;
    }


    private  IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_UNDISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleService.ACTION_SCAN);
        intentFilter.addAction(BleService.ACTION_SCAN_RET);
        intentFilter.addAction(BleService.ACTION_SCAN_BEGIN);
        return intentFilter;
    }

    /**
     * 接收BLE服务发来的广播，对数据进行处理，并分发给各个接口。
     *
     */
    private  BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BleService.ACTION_GATT_CONNECTED.equals(action)) {

            } else if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
                for (BleListener bleListener:bleListeners){
                    bleListener.onBleDisconnect();
                }
            } else if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                for (BleListener bleListener:bleListeners){
                    bleListener.onBleConnect();
                }
            }else if (BleService.ACTION_GATT_SERVICES_UNDISCOVERED.equals(action)) {

                for (BleListener bleListener:bleListeners){
                    bleListener.onBleConnectFail();
                }

            } else if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
                try {
                    byte[] data = intent.getByteArrayExtra(BleService.EXTRA_DATA);

                    for (BleListener bleListener:bleListeners){
                        bleListener.onBleRead(data);
                    }

                } catch (Exception e) {
                    // TODO: handle exception
                }
            }else if (BleService.ACTION_SCAN_RET.equals(action)){
                final String[] temps = intent.getStringArrayExtra(BleService.EXTRA_DATA);
                String[] names = new String[temps.length];
                final String[] macs = new String[temps.length];
                int i = 0;
                for (String temp:temps)
                {
                    names[i] = temp.substring(0,temp.indexOf(':'));
                    macs[i] = temp.substring(temp.indexOf(':')+1,temp.length());
                    i++;
                }

                for (BleListener bleListener:bleListeners){
                    bleListener.onBleScanRet(names,macs);
                }

                if (mUiContext!=null)
                {
                    if (showScanDialog){
                        if (scanProgressDialog!=null&&scanProgressDialog.isShowing())
                            scanProgressDialog.dismiss();

                        if (temps.length>0){
                            AlertDialog.Builder  ab = new AlertDialog.Builder(mUiContext);
                            ab.setTitle("附近的设备");
                            ab.setItems(temps, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String mac = macs[which];
                                    connect(mac);
                                }
                            });
                            if (scanRetDialog!=null&&scanRetDialog.isShowing()){scanRetDialog.dismiss();scanRetDialog = null;}
                            scanRetDialog = ab.create();
                            scanRetDialog.show();
                        }else{
                            Toast.makeText(mUiContext,"没有找到设备",Toast.LENGTH_SHORT).show();
                        }

                    }
                }

            }else if (BleService.ACTION_SCAN.equals(action)){
                String temp = intent.getStringExtra(BleService.EXTRA_DATA);
                String name = temp.substring(0,temp.indexOf(':'));
                String mac = temp.substring(temp.indexOf(':')+1,temp.length());

                for (BleListener bleListener:bleListeners){
                    bleListener.onBleScan(name,mac);
                }

                //此处补充算法，使得扫描到的设备能够动态显示出来。



            }else if (BleService.ACTION_SCAN_BEGIN.equals(action)){

                for (BleListener bleListener:bleListeners){
                    bleListener.onBleScanBegin();
                }

                if (mUiContext!=null)
                {
                    if (showScanDialog){
                        scanProgressDialog = new ProgressDialog(mUiContext);
                        scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        scanProgressDialog.setTitle("正在搜索设备");
                        scanProgressDialog.setCancelable(true);
                        //点击对话框后调用。
                        scanProgressDialog.setButton(ProgressDialog.BUTTON_POSITIVE, "停止", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                stopScan();
                                Log.e(TAG,"press the stop button");
                            }
                        });
                        //点击后退按钮时调用
                        scanProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                stopScan();
                                Log.e(TAG,"the dialog is cancel");
                            }
                        });

                        scanProgressDialog.show();


                    }
                }
            }
        }
    };

    public void scan()
    {
        Bundle bundle = new Bundle();
        bundle.putInt(BleService.ORDER,BleService.CMD_SCAN);
        bundle.putInt("time",8);
        sendCMD(bundle);

        showScanDialog = false;
    }
    /**
     * 扫描设备
     * @param time 扫描的时间周期
     */
    public void scan(int time)
    {
        Bundle bundle = new Bundle();
        bundle.putInt(BleService.ORDER,BleService.CMD_SCAN);
        bundle.putInt("time",time);
        bundle.putBoolean("show",true);
        sendCMD(bundle);

        showScanDialog = false;
    }

    public void scan(Context context, final int time, Boolean showDialog)
    {
        Bundle bundle = new Bundle();
        bundle.putInt(BleService.ORDER,BleService.CMD_SCAN);
        bundle.putInt("time",time);
        sendCMD(bundle);

        mUiContext = null;
        mUiContext = context;
        showScanDialog = showDialog;
    }

    /**
     * 主动调用停止扫描操作
     */
    public void stopScan()
    {
        Bundle bundle = new Bundle();
        bundle.putInt(BleService.ORDER,BleService.CMD_SCAN_STOP);
        sendCMD(bundle);
    }

    /**
     * 重新连接，直接连接上一次连接的设备，
     * 无论设备是否存在
     */
    public void reconnect()
    {
        Bundle bundle = new Bundle();
        bundle.putInt(BleService.ORDER,BleService.CMD_RECONNECT);
        sendCMD(bundle);

    }


    public void connect(String mac){
        Bundle bundle = new Bundle();
        bundle.putInt(BleService.ORDER,BleService.CMD_CONNECT);
        bundle.putString("mac",mac);
        sendCMD(bundle);
    }

    public void disconnect()
    {
        Bundle bundle = new Bundle();
        bundle.putInt(BleService.ORDER,BleService.CMD_DISCONNECT);
        sendCMD(bundle);

    }

    public void write(byte[] src){
        Bundle bundle = new Bundle();
        bundle.putInt(BleService.ORDER,BleService.CMD_WRITE);
        bundle.putByteArray("data",src);
        sendCMD(bundle);
    }

    public void destroy()
    {
        Bundle bundle = new Bundle();
        bundle.putInt(BleService.ORDER,BleService.CMD_DESTROY);
        sendCMD(bundle);

        destroyThread();
    }

    private void destroyThread()
    {
        if (watchService!=null){
            watchServiceFlag = false;
            watchService.interrupt();
        }
        watchService = null;
    }

    public Boolean setUUID(String serviceUUID,String readUUID,String writeUUID,String configUUID)
    {
        if (serviceUUID==null||readUUID==null||writeUUID==null)return false;

        if (readUUID.equals(writeUUID))
            Dev.isRWsame = true;
        if (configUUID==null)
        {
            Dev.needConfig = false;
        }
        Dev.setSERVICE(UUID.fromString(serviceUUID));
        Dev.setNOTIFY(UUID.fromString(readUUID));
        Dev.setWRITE(UUID.fromString(writeUUID));
        if (Dev.needConfig)
            Dev.setCONFIG(UUID.fromString(configUUID));
        return true;
    }

}
