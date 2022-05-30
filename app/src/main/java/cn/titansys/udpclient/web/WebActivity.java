package cn.titansys.udpclient.web;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import cn.titansys.udpclient.R;

public class WebActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
// 获取意图对象
        Intent intent = getIntent();
        //获取传递的值
        String str = intent.getStringExtra("URL");
        WebView mWebview = findViewById(R.id.webView);
        // 也可以通过 new 的形式创建 WebView
        mWebview.loadUrl(str);
    }
}