package com.xs.myble;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.xs.libble.Ble;
import com.xs.libwc.SJSZ;
import com.xs.libwc.WC;
import com.xs.libwc.ZTCX;
import com.xs.libwc.ZTHB;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements Ble.BleListener{

    Button btn1,btn2,btn3,btn4,btn5;
    TextView textView;
    Ble ble;
    PD pd;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ble = Ble.getInstance(getApplicationContext());

        textView = (TextView)findViewById(R.id.textView);
        btn1 = (Button)findViewById(R.id.button);
        btn2 = (Button)findViewById(R.id.button2);
        btn3 = (Button)findViewById(R.id.button3);
        btn4 = (Button)findViewById(R.id.button4);
        btn5 = (Button)findViewById(R.id.button5);

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ble.scan(5);
                pd = new PD(MainActivity.this, "正在搜索终端", new PD.Callback() {
                    @Override
                    public void chaoshi() {
                        Toast.makeText(MainActivity.this,"未搜索到设备",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void back() {

                    }
                });
                pd.show(8);//超时时间为10秒
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ble.disconnect();
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ble.reconnect();
            }
        });
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getState();
            }
        });
        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTime();
            }
        });


    }

    public void getState()
    {

        ZTCX ztcx = new ZTCX();
        ble.write(ztcx.getSrc());
        setTextView(Arrays.toString(ztcx.getSrc()));
        //onBleRead(new ZTYD(54,0).getSrc());//屏蔽即可
    }
    public void setTime()
    {
        Calendar c = Calendar.getInstance(Locale.CHINA);
        Date d = c.getTime();
        SJSZ sjsz = new SJSZ(d.getYear()-100,d.getMonth()+1,d.getDate(),d.getHours(),d.getMinutes(),d.getSeconds());
        ble.write(sjsz.getSrc());
        setTextView(Arrays.toString(sjsz.getSrc()));
        //onBleRead(new SJYD(16,8,9,12,23,52).getSrc());//屏蔽即可
    }

    @Override
    protected void onResume() {
        super.onResume();
        ble.setOnBleListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ble.removeBleListener(this);
    }

    @Override
    public void finish() {

        //ble.closeBluetooth();
        super.finish();
    }

    @Override
    public void onBleConnect() {
        Toast.makeText(MainActivity.this,"connect",Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getState();
                        }
                    });

                    try{Thread.sleep(5000);}catch (Exception e){e.printStackTrace();}
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setTime();
                        }
                    });

                    try{Thread.sleep(5000);}catch (Exception e){e.printStackTrace();}

                }
            }
        });
    }

    @Override
    public void onBleDisconnect() {
        Toast.makeText(MainActivity.this,"disconnect",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBleConnectFail() {
        Toast.makeText(MainActivity.this,"fail",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBleRead(byte[] data) {
        Toast.makeText(MainActivity.this, Arrays.toString(data),Toast.LENGTH_SHORT).show();
        /*
        double w = Math.random();
        if (w>0.9){
            ZTHB zthb = new ZTHB(1,67);
            data = zthb.getSrc();
        }else if (w>0.8){
            SJYD sjyd = new SJYD(16,8,9,12,23,52);
            data = sjyd.getSrc();
        }else if (w>0.7){
            ZTYD ztyd = new ZTYD(1,67);
            data = ztyd.getSrc();
        }else if (w>0.6){
            SJSZ sjsz = new SJSZ(16,8,9,12,23,52);
            data = sjsz.getSrc();
        }else if (w>0.5){
            ZTCX zthb = new ZTCX();
            data = zthb.getSrc();
        }*/

        if (data[0]== 126){
            ZTHB zthb = new ZTHB(1,67);
            data = zthb.getSrc();
        }

        setTextView(Arrays.toString(data));

        //以下进行协议解码
        WC wc = new WC(data);
        wc.analyse(new WC.WClistener() {
            @Override
            public void ZTYD(int power, int alarm) {
                setTextView("ZTYD "+ power+" "+alarm);
            }

            @Override
            public void ZTHB(int power, int alarm) {
                setTextView("ZTHB "+ power+" "+alarm);
            }

            @Override
            public void SJYD(int year, int month, int day, int hour, int min, int second) {
                setTextView("SJYD 20"+ year+"-"+month+"-"+day+" "+hour+":"+min+":"+second);
            }

            @Override
            public void YD(byte[] src) {
                setTextView(Arrays.toString(src));
                ble.getInstance(MainActivity.this).write(src);
            }
        });
    }

    Dialog scanDialog;

    @Override
    public void onBleScan(String name, String mac) {

    }


    @Override
    public void onBleScanRet(String[] names,final String[] macs) {
        //Toast.makeText(MainActivity.this,mac,Toast.LENGTH_SHORT).show();
        if (pd!=null&&pd.isShowing())
            pd.cancle();

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("请选择终端");
        builder.setItems(macs, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String mac = macs[which];
                ble.connect(mac);//连接指定MAC地址。

            }
        });
        if (scanDialog!=null&&scanDialog.isShowing())
            scanDialog.cancel();
        scanDialog = builder.create();
        scanDialog.show();

    }

    @Override
    public void onBleScanBegin() {

    }

    public void setTextView(String string)
    {
        textView.setText(string+"\n"+textView.getText());
    }
}
