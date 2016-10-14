package com.xs.myble;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.xs.libble.Ble;

import java.util.Arrays;



/**
 * Created by linrh on 2016/9/9.
 */
public class TestActivity extends AppCompatActivity {

    Spinner order;
    Button run;
    TextView show;
    public static Handler show_text;//该句柄用来显示当前的状态
    Ble.BleListener bleListener;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        order = (Spinner)findViewById(R.id.spinner);
        run  = (Button)findViewById(R.id.button);
        show = (TextView)findViewById(R.id.textView);

        String[] mItems = {
                "扫描",
                "扫描并显示结果",
                "重连上次设备",
                "断开连接",
                "销毁服务",
                "查看帮助文档"};
        ArrayAdapter adapter=new ArrayAdapter(this,android.R.layout.simple_spinner_item, mItems);
        order.setAdapter(adapter);

        run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //read the spinner's order;
                int i = order.getSelectedItemPosition();
                String text  = (String) order.getSelectedItem();
                switch (text) {
                    case "扫描":
                        showText("扫描");
                        Ble.getInstance().scan(5);
                        break;
                    case "扫描并显示结果":
                        showText("扫描并显示结果");
                        Ble.getInstance().scan(TestActivity.this,5,true);
                        break;
                    case "重连上次设备":
                        showText("重连上次设备");
                        Ble.getInstance().reconnect();
                        break;
                    case "断开连接":
                        Ble.getInstance().disconnect();
                        break;

                    case "销毁服务":
                        Ble.getInstance().destroy();
                        break;
                    case "查看帮助文档":
                        startActivity(new Intent(TestActivity.this, HelpActivity.class));
                        break;
                    default:
                        break;
                }
            }
        });

        show_text = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                int what = msg.what;
                String order = msg.getData().getString("order");
                if(order==null)order="";
                String text = msg.getData().getString("text");
                if (text==null)text="";
                show.setText(what+":"+order+":"+text+"\n"+show.getText().toString());

                return false;
            }
        });



         bleListener = new Ble.BleListener() {
            @Override
            public void onBleConnect() {
                showText("onBleConnect");
            }

            @Override
            public void onBleDisconnect() {
                showText("onBleDisconnect");
            }

            @Override
            public void onBleConnectFail() {
                showText("onBleConnectFail");
            }

            @Override
            public void onBleRead(byte[] data) {
                showText("onBleRead", Arrays.toString(data));
            }

            @Override
            public void onBleScan(String name, String mac) {
                showText("单个扫描",name+"["+mac+"]");
            }

            @Override
            public void onBleScanRet(String[] names, String[] macs) {
                for (int i = 0;i<macs.length;i++){
                    showText("整体扫描",names[i]+"["+macs[i]+"]");
                }
            }

            @Override
            public void onBleScanBegin() {
                showText("开始扫描");
            }
        };

        Ble.getInstance(TestActivity.this).setOnBleListener(bleListener);//设置监听
        Ble.getInstance().reconnect();//重连上次设备
    }

    public  static void showText(String order,String text){
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("order",order);
        bundle.putString("text",text);
        msg.setData(bundle);
        show_text.sendMessage(msg);
    }

    public  static void showText(String order){
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("order",order);
        msg.setData(bundle);
        show_text.sendMessage(msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Ble.getInstance().removeBleListener(bleListener);
    }
}
