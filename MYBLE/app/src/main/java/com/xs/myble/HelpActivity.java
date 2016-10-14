package com.xs.myble;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * Created by linrh on 2016/10/13.
 * 加载assets文件夹中的HTML文件。
 */
public class HelpActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        WebView webView = (WebView)findViewById(R.id.webView);
        webView.loadUrl("file:///android_asset/help.html");
    }
}
