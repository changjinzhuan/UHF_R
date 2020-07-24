package com.handheld.uhfr.kcrx.netutil;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class NetChecker extends Thread {
    private boolean isstart=false;
    private boolean isconnect=false;
    private Handler mHandler;

    public NetChecker(Handler handler)
    {
        mHandler=handler;
    }
   public void pause()
   {
       isstart=false;
   }


    @Override
    public void run() {
        isstart=true;
        while(isstart)
        {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            boolean nowstate=isWirePluggedIn();
            if(nowstate!=isconnect)
            {
                isconnect=nowstate;
                Message msg = new Message();
                msg.what = 1;
                Bundle b = new Bundle();
                b.putBoolean("netstate",isWirePluggedIn());
                msg.setData(b);
                mHandler.sendMessage(msg);
            }

        }
    }

    public String execCommand(String command) {
        Runtime runtime;
        Process proc = null;
        StringBuffer stringBuffer = null;
        try {
            runtime = Runtime.getRuntime();
            proc = runtime.exec(command);
            stringBuffer = new StringBuffer();
            if (proc.waitFor() != 0) {
                System.err.println("exit value = " + proc.exitValue());
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    proc.getInputStream()));

            String line = null;
            while ((line = in.readLine()) != null) {
                stringBuffer.append(line + " ");
            }

        } catch (Exception e) {
            System.err.println(e);
        } finally {
            try {
                proc.destroy();
            } catch (Exception e2) {
            }
        }
        return stringBuffer.toString();
    }
    //判断网线拔插状态
    //通过命令cat /sys/class/net/eth0/carrier，如果插有网线的话，读取到的值是1，否则为0
    public boolean isWirePluggedIn(){
       // String state= execCommand("cat /sys/class/net/wlan0/carrier");
        String state= execCommand("cat /sys/class/net/eth0/carrier");
        if(state.trim().equals("1")){  //有网线插入时返回1，拔出时返回0
            return true;
        }
        return false;
    }
}
