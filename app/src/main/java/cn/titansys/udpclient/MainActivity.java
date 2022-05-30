package cn.titansys.udpclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.titansys.udpclient.Radar.RadarView;
import cn.titansys.udpclient.tdialog.TDialog;
import cn.titansys.udpclient.tdialog.base.BindViewHolder;
import cn.titansys.udpclient.tdialog.listener.OnBindViewListener;
import cn.titansys.udpclient.tdialog.listener.OnViewClickListener;
import cn.titansys.udpclient.web.WebActivity;

public class MainActivity extends AppCompatActivity {

    private DatagramSocket udpSocket = null;
    //private static final int PhonePort = 7171;//udp手机端口号
    private DatagramPacket packet;
    private volatile boolean stopReceiver;
    private String LocalAddress = "TITAN";
    private String messageTemp = null;
    private RadarView radarView;
    private CheckBox cb_search;
    private EditText edt_port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        radarView = findViewById(R.id.radar);
        edt_port= findViewById(R.id.edt_port);
        edt_port.setRawInputType(Configuration.KEYBOARD_QWERTY);

        stopReceiver = false;
        cb_search = (CheckBox) findViewById(R.id.cb_search);
        cb_search.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Log.e("CheckBox:::","选中");
                    radarView.start();
                    receiveMessage(tf_port());
                    cb_search.setText("停止搜索");
                    stopReceiver=false;
                }else{
                    Log.e("CheckBox:::","取消");
                    cb_search.setText("重新开始");
                    radarView.stop();
                    //没有问题后操作完成
                    stopReceiver=true;
                }
            }
        });
    }

    //强转并判断是否符合条件
    private int tf_port(){
        if(isNumeric(edt_port.getText().toString())){
            int port_i=Integer.parseInt(edt_port.getText().toString());
            return port_i;
        }else {
            return 0;
        }
    }

    private boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if( !isNum.matches() ){
            return false;
        }
        return true;
    }

    //扫描本地信息
    private void receiveMessage(int phonePort) {

        if(phonePort==0){
            Toast.makeText(MainActivity.this,"端口号获取失败",Toast.LENGTH_LONG).show();
        }


        Log.e("receiveMessage", "开始扫描本地"+phonePort);
        messageTemp = null;
        //开一个子线程扫描本地，扫描到了，线程就自己关闭了
        new Thread() {
            public void run() {
                try {
                    //会有端口占用的问题，这样写就不会有什么问题了
                    if (udpSocket == null) {
                        udpSocket = new DatagramSocket(null);
                        udpSocket.setReuseAddress(true);
                        udpSocket.bind(new InetSocketAddress(phonePort));//接收者地址
                    }
                } catch (SocketException e) {
                    Log.e("SocketException-1", e.toString());
                    //做一下异常处理
                }
                while (!stopReceiver) {
                    Log.e("info:", "进入循环");
                    byte[] receBuf = new byte[1024];
                    packet = new DatagramPacket(receBuf, receBuf.length);//UDP套接字
                    try {
                        udpSocket.receive(packet);//blocked here
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String address = String.valueOf(packet.getAddress());
                    String path = null;
                    try {
                        path = new String(packet.getData(), 0, packet.getLength(), "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    //Log.e("address", address);
                    if (!address.contains("null") && messageTemp == null) {//这个是去一下重，防止拿到udp数据重复
                        //下面这些调试没有出现问题
                        messageTemp = address;
                        if (messageTemp.contains("/")) {
                            LocalAddress = messageTemp.replace("/", "");
                        } else {
                            LocalAddress = messageTemp;
                        }
                        Log.e("udp::", LocalAddress+path);
                        String finalPath = path;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                radarView.stop();
                                DetailDialog(LocalAddress,"http://"+LocalAddress +"/"+ finalPath);
                            }
                        });

                        //没有问题后操作完成
                        if (socketClosed()) {
                            return;//退出该方法
                        }
                    }
                }
                Log.e("info:", "出了循环");
            }
        }.start();
    }


    private boolean socketClosed() {
        try {
            if (udpSocket != null) {
                System.out.println("内部断开socket");
                if (!udpSocket.isClosed()) {
                    udpSocket.close();
                }
                udpSocket.disconnect();
                udpSocket = null;
                return true;
            } else {
                return true;
            }
        }catch(Exception e){
            udpSocket = null;
            return true;
        }
    }


    public void DetailDialog(String ip,String str) {
        new TDialog.Builder(getSupportFragmentManager())
                .setLayoutRes(R.layout.dialog_version_upgrde)
                .setScreenWidthAspect(this, 0.7f)
                .addOnClickListener(R.id.tv_cancel, R.id.tv_confirm)
                .setDialogAnimationRes(R.style.animate_dialog_scale)
                .setOnBindViewListener(new OnBindViewListener() {   //通过BindViewHolder拿到控件对象,进行修改
                    @Override
                    public void bindView(BindViewHolder bindViewHolder) {
                        bindViewHolder.setText(R.id.tv_content, str);
                        bindViewHolder.setText(R.id.tv_title, "Server:"+ip);
                    }
                })
                .setOnViewClickListener(new OnViewClickListener() {
                    @Override
                    public void onViewClick(BindViewHolder viewHolder, View view, TDialog tDialog) {
                        switch (view.getId()) {
                            case R.id.tv_cancel:
                                radarView.start();
                                receiveMessage(tf_port());
                                tDialog.dismiss();
                                break;
                            case R.id.tv_confirm:
                                Intent intent = new Intent(MainActivity.this, WebActivity.class);
                                intent.putExtra("URL",str);
                                startActivity(intent);
                                tDialog.dismiss();
                                break;
                        }
                    }
                })
                .create()
                .show();
    }

    //region 点击隐藏键盘
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (isHideInput(view, ev)) {
                HideSoftInput(view.getWindowToken());
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    // 判定是否需要隐藏
    private boolean isHideInput(View v, MotionEvent ev) {
        if (v != null && (v instanceof EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0], top = l[1], bottom = top + v.getHeight(), right = left
                    + v.getWidth();
            if (ev.getX() > left && ev.getX() < right && ev.getY() > top
                    && ev.getY() < bottom) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    // 隐藏软键盘
    private void HideSoftInput(IBinder token) {
        if (token != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
    //endregion
}