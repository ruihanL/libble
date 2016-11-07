#  蓝牙BLE库libble使用说明





##实现性能
- 1、调用指令自动扫描周边BLE设备，能指定扫描时间，能设置是否弹出选择框。
- 2、可设定监听器，接收连接状态和接收数据。
- 3、自动保存最后一次连接成功的设备
- 4、能在启动运行时自动连接历史设备。
- 5、意外断开时能自动重连。
- 6、状态栏有蓝牙信号强度提示。


##导入方法：
- 1、复制libble.aar文件到工程任意目录下。在新的工程中，创建新的module，选择import此Module。
- 2、在工程中加入蓝牙权限
``` <uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
并添加蓝牙后台服务声明
<service android:name="com.xs.libble.BleService"/>
```


##使用方法
1、首先利用Context创建一个单例
Ble.getInstance(TestActivity.this);
其中的Context可以使用Activity的Context，也可以使用Application的Context。
以后调用命令的时候直接使用Ble.getInstance()即可。

2、新建一个BLE的监听器，用来接收BLE连接的各种信息。

    Ble.BleListener listener = new Ble.BleListener() {
    @Override
    public void onBleConnect() {
    //BLE匹配成功之后，回调此函数。
    }
    
    @Override
    public void onBleDisconnect() {
    //BLE断开之后，回调此函数。
    }
    
    @Override
    public void onBleConnectFail() {
    //BLE成功连接上，但是UUID匹配失败，会回调此函数。
    }
    
    @Override
    public void onBleRead(byte[] data) {
    //收到数据时，回调此函数。一般来说最多为20个字节。
    }
    
    @Override
    public void onBleScanBegin() {
    //调用扫描命令之后，会回调此函数。
    }
    
    @Override
    public void onBleScan(String name, String mac) {
    //扫描到设备MAC地址后，会逐个返回
    }
    
    @Override
    public void onBleScanRet(String[] names, String[] macs) {
    //扫描结束后，返回所有结果。
    }
    };

3、加入监听
Ble.getInstance().setOnBleListener(listener);
在使用结束之后，调用解除监听
Ble.getInstance().removeBleListener(listener);

4、连接上次连接成功过的设备（模块会自动保存上次连接的设备MAC地址）。此时会直接连接上次保存的MAC地址的设备，不会进行设备扫描。
Ble.getInstance().reconnect();

5、启动扫描5秒钟，但不出现扫描框。
Ble.getInstance().scan(5);
也可以使用默认的对话框显示扫描到的MAC列表。点击列表可以进行连接。
Ble.getInstance().scan(TestActivity.this,5,true);

6、写数据
Ble.getInstance().write(new byte[5]);

7、连接设备
Ble.getInstance().connect(String mac);
连接匹配需要设备的UUID，可以使用下面的指令设置UUID
public Boolean setUUID(String serviceUUID,String readUUID,String writeUUID,String configUUID)


8、断开当前的连接设备
Ble.getInstance().disconnect();

9、断开连接并关闭BLE后台服务。
Ble.getInstance().destroy();








