package com.xs.myble;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;

import java.util.Timer;

/**
 * Created by linrh on 2016/8/11.
 * 设置一个按键动作；还有超时提示。
 */
public class PD {
    public interface Callback {
        void chaoshi();
        void back();
    }
    public ProgressDialog mpd;
    public Timer timer;
    public Handler handler;
    public Context mcontext;


    public PD(Context context, String msg, final Callback callback) {
        mcontext = context;
        mpd = new ProgressDialog(context);
        mpd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mpd.setMessage(msg);
        mpd.setIndeterminate(false);
        mpd.setCancelable(true);
        mpd.setCanceledOnTouchOutside(false);

        mpd.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    callback.back();
                    mpd.cancel();
                    return true;
                }
                return false;
            }
        });

        //接收超时提醒。
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                if (mpd.isShowing())
                {
                    mpd.cancel();
                    callback.chaoshi();
                }
                return true;
            }
        });
    }
    public void show(int chaoshi)
    {
        mpd.show();
        handler.sendEmptyMessageDelayed(0,chaoshi*1000);
    }
    public void cancle()
    {
        mpd.cancel();
    }
    public Boolean isShowing()
    {
        return  mpd.isShowing();
    }
}
