package com.handheld.uhfr;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.handheld.uhfr.kcrx.netutil.NetChecker;
import com.handheld.uhfr.kcrx.netutil.MySocket_2;

import java.io.UnsupportedEncodingException;

public class Main_Lock_Activity extends AppCompatActivity {

    String logtag = "kcrx_lock";
    MySocket_2 mySocket_2;
    TextView tv_connectstate;
    //消息中心
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1://网络状态消息
                    setlog("网路状态:" + msg.getData().getBoolean("netstate"));
                    if (msg.getData().getBoolean("netstate")) {
                        //建立socket连接

                            if (mySocket_2 == null) {
                                mySocket_2 = MySocket_2.instance(mHandler);
                                mySocket_2.connect("192.168.1.105", 10000);
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(mySocket_2.isConnect())
                                {
                                    tv_connectstate.setText("业务机已连接......");
                                    tv_connectstate.setTextColor(Color.GREEN);
                                    //这里要开始通讯注册：

                                }else
                                {
                                    tv_connectstate.setText(R.string.not_connect);
                                    tv_connectstate.setTextColor(Color.RED);
                                }
                            }else
                            {

                                if(mySocket_2.isConnect())
                                {
                                    tv_connectstate.setText("业务机已连接......");
                                    tv_connectstate.setTextColor(Color.GREEN);
                                    //这里要开始通讯注册：
                                }else
                                {
                                    mySocket_2.connect("192.168.1.105", 10000);
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                    } else {
                        tv_connectstate.setText(R.string.not_connect);
                        tv_connectstate.setTextColor(Color.RED);
                    }


                    break;
                case 2://socket收到通讯组件消息
                    try {
                        setlog("收到通讯组件消息"+new String(msg.getData().getByteArray("socketmsg"),"utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    break;

//                    case INFO_MES:
//                        setlog(msg.getData().getString("message").toString());
//                        break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main__lock);

        tv_connectstate = findViewById(R.id.tv_connectstate);


        //启动网络监控
        NetChecker netChecker = new NetChecker(mHandler);
        netChecker.start();

    }
    private void socketReg()//通讯组件注册
    {

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mySocket_2 != null) {
            mySocket_2.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //回到界面，需要启动网络检查
    }

    private void setlog(String log) {
        Log.e(logtag, log);
    }
}
