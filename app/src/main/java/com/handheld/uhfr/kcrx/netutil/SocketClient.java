package com.handheld.uhfr.kcrx.netutil;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class SocketClient {

    Socket socket=null;
    private BufferedReader reader=null;
    private BufferedWriter writer=null;
    private BufferedReader reader2=null;

    private boolean running=false;
    public SocketClient()
    {

    }
    public void connect()
    {
        try
        {
            socket=new Socket("192.168.1.105", 10000);
            reader = new BufferedReader(new InputStreamReader(System.in));
            reader2=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            new MyRuns(reader).run();
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    void readSocketInfo(BufferedReader reader){
        new Thread(new MyRuns(reader)).start();
    }
    public void sendMsg(String msg)
    {
        try {
            writer.write(msg);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void stop()
    {
        running=false;
    }
    class MyRuns implements Runnable{

        BufferedReader reader;

        public MyRuns(BufferedReader reader) {
            super();
            this.reader = reader;
        }

        public void run() {
            running=true;
            while(running)
            {
                try {
                    String lineString="";
                    while( (lineString = reader.readLine())!=null ){
                        Log.d("kcrx","收到服务器消息:"+lineString);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d("kcrx","连接已断开");

        }

    }

}
