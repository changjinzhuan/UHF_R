package com.handheld.uhfrdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.BRMicro.Tools;
import com.handheld.uhfr.R;
import com.handheld.uhfr.UHFRManager;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.PsamCmdUtil;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.cn.kcrx.bean.TagEpcData;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.cn.kcrx.bean.TagUserdata;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.cn.kcrx.bean.UserTraceData;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.rfidtool.EpcReader;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.rfidtool.UserReader;
import com.handheld.uhfrdemo.cn.kcrxorg.uhfutil.LockHelper;
import com.uhf.api.cls.Reader;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.Error;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RfidLockActivity extends AppCompatActivity implements View.OnClickListener {
    String logtag = "kcrxlog";
    private Button buttonopen;
    private Button buttontest;
    private Button btn_demo;
    private Button buttonactive;
    private Button buttontrace;
    private Button buttonrecovery;
    private Button buttondelete;
    private Button buttonunlockgetrs;
    private Button buttonlockgetrs;

    private Button buttonunlock;
    private Button buttonlock;
    private Button btn_reset;
    private Spinner sp_var;
    private Button btn_tracecmd;
    private EditText ev_tracenum;
    private CheckBox checkbox_clearnum;

    private TextView tvinfo;

    private ScrollView scorllinfo;
    //  private Timer timer;

    private PsamCmdUtil psam;

    private int psamCard = 1;//PSAM卡座号

    private Handler mHandler;
    private static final int INFO_MES = 0;

    private boolean isMulti = false;// multi mode flag
    private int allCount = 0;// inventory count

    //   SerialPort mSerialPort;
    public static UHFRManager mUhfrManager;//uhf
    private SharedPreferences mSharedPreferences;
    LockHelper lockHelper;

    byte[] varbytes={0x01,0x02,0x03,0x04,0x05,0x06};
    String[] varnames={"100元","50元","20元","10元","5元","1元"};

    byte activeb=0x01;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_rfid_lock);

        btn_reset=findViewById(R.id.btn_reset);
        buttonopen = findViewById(R.id.button_open);
        buttontest = findViewById(R.id.button_test);
        btn_demo = findViewById(R.id.btn_demo);
        buttonactive = findViewById(R.id.btn_active);
        buttontrace = findViewById(R.id.btn_trace);
        buttonrecovery=findViewById(R.id.btn_recovery);
        buttondelete=findViewById(R.id.btn_delete);
        buttonunlockgetrs=findViewById(R.id.btn_unlockgetrs);
        buttonlockgetrs=findViewById(R.id.btn_lockgetrs);
        scorllinfo = findViewById(R.id.scroll_info);
        tvinfo = findViewById(R.id.tv_info);
        sp_var=findViewById(R.id.sp_var);
        btn_tracecmd=findViewById(R.id.btn_tracecmd);
        ev_tracenum=findViewById(R.id.ev_tracenum);
        buttonunlock=findViewById(R.id.btn_unlock);
        buttonlock=findViewById(R.id.btn_lock);
        checkbox_clearnum=findViewById(R.id.checkbox_clearnum);//清除日志操作数限制

        buttonopen.setOnClickListener(this);
        buttontest.setOnClickListener(this);
        btn_demo.setOnClickListener(this);
        buttonactive.setOnClickListener(this);
        buttontrace.setOnClickListener(this);
        buttonrecovery.setOnClickListener(this);
        buttondelete.setOnClickListener(this);
        buttonlockgetrs.setOnClickListener(this);
        buttonunlockgetrs.setOnClickListener(this);
        btn_reset.setOnClickListener(this);
        btn_tracecmd.setOnClickListener(this);
        buttonunlock.setOnClickListener(this);
        buttonlock.setOnClickListener(this);
        //选择全选
        ev_tracenum.setSelectAllOnFocus(true);
        //激活券别列表选择
        ArrayAdapter<String> aa = new ArrayAdapter<String>(this, R.layout.sp_items,R.id.tv_stackInfo,varnames);
        sp_var.setAdapter(aa);
        sp_var.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activeb=varbytes[position];

                Log.e("test","选择激活的是"+varnames[position]+" 值为："+varbytes[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //设置焦点
        btn_demo.setFocusable(true);
        btn_demo.setFocusableInTouchMode(true);
        btn_demo.requestFocus();
        btn_demo.requestFocusFromTouch();

        //自动滚动
        scorllinfo.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scorllinfo.post(new Runnable() {
                    @Override
                    public void run() {
                        scorllinfo.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

        //mSerialPort=new SerialPort();切换不好使

        Util.initSoundPool(this);



        //   mSharedPreferences = getSharedPreferences("UHF",MODE_PRIVATE);

//        int rs= UHfData.UHfGetData.OpenUHf("/dev/ttyMT1", 57600);
//        if(rs!=0)
//        {
//            Log.e(logtag,"天线模块连接失败!");
//        }else
//        {
//            Log.e(logtag,"天线模块连接成功!rs="+rs);
//        }
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        setlog("读取到epc=" + msg.getData().getString("epc") + " 信号:" + msg.getData().getString("rssi"));
                        sendMes(mHandler, "包号:" + EpcReader.readEpc(msg.getData().getString("epc")).getTagid());
                        break;
                    case INFO_MES:
                        setlog(msg.getData().getString("message").toString());
                        break;

                }
                super.handleMessage(msg);
            }
        };

        lockHelper=new LockHelper(mHandler);
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.btn_unlock:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        operateLockGetrs(mHandler, false);
                    }
                }).start();
                break;
            case R.id.btn_lock:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        operateLockGetrs(mHandler, true);
                    }
                }).start();
                break;
            case R.id.button_open:
                setlog("关锁键按下F1,开始执行关锁**********************");
//                rs= LockHelper.Lockws("31323334","31323334",true);
//                setlog(rs.value()+"");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        operateLockGetrs(mHandler, true);
                    }
                }).start();
                break;
            case R.id.button_test:
                setlog("开锁键按下F2,开始执行开锁**********************");
//                rs= LockHelper.Lockws("31323334","31323334",false);
//                setlog(rs.value()+"");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        operateLockGetrs(mHandler, false);
                    }
                }).start();
                break;
            case R.id.btn_lockgetrs:
                tvinfo.setText("");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        operateLockWsGetrs(mHandler, true);
                    }
                }).start();

                break;
            case R.id.btn_unlockgetrs:
                tvinfo.setText("");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        operateLockWsGetrs(mHandler, false);
                    }
                }).start();

                break;
            case R.id.btn_active:
                tvinfo.setText("");
                sendMes(mHandler,"激活按钮按下，开始写入激活指令");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.e("test","激活指令:"+activeb);
                            LockHelper.LOCK_ERR rs = lockHelper.RFID_Active(activeb);
                            if (rs.equals(LockHelper.LOCK_ERR.LOCK_OK)) {
                                Util.play(2,0);
                                sendMes(mHandler,"激活指令执行成功");
                            }else
                            {
                                sendMes(mHandler,"激活指令执行失败"+rs);
                                Util.play(3,0);
                            }
                        }catch (Exception e1)
                        {
                            sendMes(mHandler,"指令执行失败"+e1.getMessage()+"\r\n");
                        }

                    }
                }).start();

                break;
            case R.id.btn_trace://追溯user区命令
                tvinfo.setText("");
                sendMes(mHandler,"追溯user区命令按钮按下，开始追溯user区");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try
                        {
                            final String[] tracedata = {""};

                            tracedata[0] = lockHelper.RFID_Trace();

                            if (!tracedata[0].contains("_ERR")) {
                                 Util.play(2,0);
                                TagUserdata tagUserdata= UserReader.readTagUser(Tools.HexString2Bytes(tracedata[0]));
                                UserTraceData[] userTraceDatas= tagUserdata.getUserTraceData();
                                for(int i=0;i<userTraceDatas.length;i++)
                                {
                                    if(userTraceDatas[i].getCommandid().startsWith("B"))
                                    {
                                        sendMes(mHandler,"第"+(i+1)+"条记录："+" 命令码:"+userTraceDatas[i].getCommandid()+"操作人1:"+userTraceDatas[i].getOperator1()+"操作人2:"+userTraceDatas[i].getOperator2()+"时间:"+userTraceDatas[i].getOpdatetime());
                                    }else
                                    {
                                        sendMes(mHandler,"第"+(i+1)+"条记录："+"命令码:"+userTraceDatas[i].getCommandid());
                                    }
                                }

                                sendMes(mHandler,"EPC备份区为："+tagUserdata.getTagEpcDatabak().getEpcString());
                                sendMes(mHandler,"硬件版本为:" + tagUserdata.getHardwareVersion());
                                sendMes(mHandler,"软件版本为:" + tagUserdata.getSoftVersion());
                                sendMes(mHandler,"日志计数为:" + tagUserdata.getLogCount());

                                Boolean[] booleans=tagUserdata.getUserErrorData();
                                StringBuilder sb=new StringBuilder();
                                for(int i=0;i<booleans.length;i++)
                                {
                                    sb.append((booleans.length-i-1)+":"+booleans[i]+" ");
                                }
                                sendMes(mHandler,"32位异常标志为："+sb.toString());
                            } else {
                                Util.play(3,0);
                                sendMes(mHandler, "读取用户区数据失败:" + tracedata[0]);
                            }
                        }catch (Exception e1)
                        {
                            Util.play(3,0);
                            sendMes(mHandler,"读取用户区数据失败:"+e1.getMessage()+"\r\n");
                        }
                    }
                }).start();
                break;
            case R.id.btn_recovery:
                tvinfo.setText("");
                sendMes(mHandler,"恢复按钮按下，开始写入恢复指令");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try
                        {
                            LockHelper.LOCK_ERR rs =lockHelper.RFID_Recovery("31323334","31323334");
                            if (rs.equals(LockHelper.LOCK_ERR.LOCK_OK)) {
                                sendMes(mHandler,"恢复指令执行成功");
                                Util.play(2,0);
                            }else
                            {
                                sendMes(mHandler,"恢复指令执行失败"+rs);
                                Util.play(3,0);
                            }
                        }catch (Exception e1)
                        {
                            sendMes(mHandler,"指令执行失败"+e1.getMessage()+"\r\n");
                        }

                    }
                }).start();


                break;
            case R.id.btn_delete:
                tvinfo.setText("");
                setlog("日志清除按钮按下，开始写入删除指令");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try
                        {

                            LockHelper.LOCK_ERR rs =lockHelper.RFID_Delete("31323334","31323334",checkbox_clearnum.isChecked());
                            if (rs.equals(LockHelper.LOCK_ERR.LOCK_OK)) {
                                Util.play(2,0);
                                sendMes(mHandler,"志清除指令执行成功");
                            }else
                            {
                                sendMes(mHandler,"志清除指令执行失败"+rs);
                                Util.play(3,0);
                            }
                        }catch (Exception e1)
                        {
                            sendMes(mHandler,"指令执行失败"+e1.getMessage()+"\r\n");
                        }
                    }
                }).start();
                break;
            case R.id.btn_reset:
                tvinfo.setText("");
                setlog("重置按钮按下，开始写入重置指令");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try
                        {
                            LockHelper.LOCK_ERR rs =lockHelper.RFID_Reset();
                            if (rs.equals(LockHelper.LOCK_ERR.LOCK_OK)) {
                                Util.play(2,0);
                                sendMes(mHandler,"重置指令执行成功");
                            }else
                            {
                                sendMes(mHandler,"重置指令执行失败"+rs);
                                Util.play(3,0);
                            }
                        }catch (Exception e1)
                        {
                            sendMes(mHandler,"重置指令执行失败"+e1.getMessage()+"\r\n");
                        }
                    }
                }).start();

                break;
            case R.id.btn_demo:
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        int num=Integer.parseInt(ev_tracenum.getText()+"");
//                       lockHelper.changePort(15);
//                    }
//                }).start();
                Intent mainintent=new Intent(this, MainActivity.class);
                startActivity(mainintent);
                break;
            case R.id.btn_tracecmd:
                tvinfo.setText("");
                setlog("追溯指令按钮按下，开始写入追溯指令");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int num=Integer.parseInt(ev_tracenum.getText()+"");
                        LockHelper.LOCK_ERR rs=lockHelper.RFID_TraceCmd(num);
                        if (rs.equals(LockHelper.LOCK_ERR.LOCK_OK)) {
                            Util.play(2,0);
                            sendMes(mHandler,"追溯指令执行成功");
                        }else
                        {
                            sendMes(mHandler,"追溯指令执行失败"+rs);
                            Util.play(3,0);
                        }
                    }
                }).start();
                break;
        }
    }

    /**
     * key receiver
     */
    private long startTime = 0;
    private boolean keyUpFalg = true;
    private BroadcastReceiver keyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            int keyCode = intent.getIntExtra("keyCode", 0);
//            // H941
//            if (keyCode == 0) {
//                keyCode = intent.getIntExtra("keycode", 0);
//            }
//            boolean keyDown = intent.getBooleanExtra("keydown", false);
//            if (keyUpFalg && keyDown && System.currentTimeMillis() - startTime > 500) {
//                keyUpFalg = false;
//                startTime = System.currentTimeMillis();
//                if (keyCode == KeyEvent.KEYCODE_F1) {
//                    //  sendMes(mHandler, "当前网络连接状态：" + isWirePluggedIn());
//                    onClick(buttonclear);
//                    onClick(buttonopen);
//                } else if (keyCode == KeyEvent.KEYCODE_F2) {
//                    //  sendMes(mHandler, "当前网络连接状态：" + isWirePluggedIn());
//                    onClick(buttonclear);
//                    onClick(buttontest);
//                }
//                return;
//            } else if (keyDown) {
//                startTime = System.currentTimeMillis();
//            } else {
//                keyUpFalg = true;
//            }

        }
    };


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
    public boolean isWirePluggedIn() {
        String state = execCommand("cat /sys/class/net/wlan0/carrier");
        if (state.trim().equals("1")) {  //有网线插入时返回1，拔出时返回0
            return true;
        }
        return false;
    }


    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.rfid.FUN_KEY");
        registerReceiver(keyReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(keyReceiver);
    }

    private void readTest() {
//        String EPCStr="3B9CD92A11000016013C9340";
//        int selecteded=3;
//        byte[] epcb=UHfData.UHfGetData.hexStringToBytes(EPCStr);
//        int wordstr=Integer.valueOf("76");
//        byte[] word = cn.pda.serialport.Tools.intToByte(wordstr);
//        UHfData.setuhf_id(EPCStr);
//        String RfidPwd="EA68FBAB";
//        Log.e("kcrxtag","开始读取标签user区");
//        Log.e("kcrxtag","Epcstr="+EPCStr);
//        Log.e("kcrxtag","selectedEd="+selecteded);
//        Log.e("kcrxtag","cwordPtr="+wordstr);
//        Log.e("kcrxtag","lenStr="+4);
//        Log.e("kcrxtag","PWDStr="+RfidPwd);
//
//        int rs= UHfData.UHfGetData.Read6C((byte)(EPCStr.length()/4),
//                epcb,
//                (byte)selecteded,
//                word,
//                Byte.valueOf(4+""),
//                UHfData.UHfGetData.hexStringToBytes(RfidPwd));
//
//        String temp = UHfData.UHfGetData
//                .bytesToHexString(UHfData.UHfGetData.getRead6Cdata(), 0, Byte.valueOf(4+"") * 2)
//                .toUpperCase();
//        Log.e("kcrxtag","temp="+temp);
//        if(rs!=0)
//        {
//            Log.e("kcrxtag","指令区读取失败!");
//
//        }else
//        {
//            Log.e("kcrxtag","指令区读取成功!rs="+rs);
//        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUhfrManager != null) {
            mUhfrManager.close();
        }
        //   UHfData.UHfGetData.CloseUHf();
    }

    String epc = "481F22811100008200662700";
    private boolean operateLockGetrs(Handler mHandler, boolean lock)
    {
        boolean thisflag = false;
        long startTime = System.currentTimeMillis();
        long allstartTime = System.currentTimeMillis();
        sendMes(mHandler, "*********************************************************");
        sendMes(mHandler, "1、开始扫描标签...");
        mUhfrManager = UHFRManager.getIntance();

        //******************************************test start
        mUhfrManager.setPower(5, 5);
        List<Reader.TAGINFO> list1;
        list1 = mUhfrManager.tagInventoryByTimer((short) 50);

        String hasElecEpc = "";
        List<Reader.TAGINFO> list2;
        if (list1 != null && list1.size() > 0) {//

            list2 = new ArrayList<Reader.TAGINFO>();
            for (Reader.TAGINFO tfs : list1) {
                byte[] epcdata = tfs.EpcId;
                epc = Tools.Bytes2HexString(epcdata, epcdata.length);
                TagEpcData ted = EpcReader.readEpc(epc);
                if (ted != null) {
                    // if(ted.getHasElec())//如果是上电标签
               //     if (ted.getTagid() > 1000000000L&&ted.getHasElec())//如果存在大于0的包号
                    if (ted.getTagid() > 1L&&ted.getHasElec())
                    {
                        list2.add(tfs);
                        hasElecEpc = epc;
                        int rssi = tfs.RSSI;
                        sendMes(mHandler, "epc=" + epc);
                        Message msg = new Message();
                        msg.what = 1;
                        Bundle b = new Bundle();
                        b.putString("epc", epc);
                        b.putString("rssi", rssi + "");
                        msg.setData(b);
                        mHandler.sendMessage(msg);
                    }
                }
            }
        } else {
            sendMes(mHandler, "未读取到标签");
            Util.play(3, 0);
            return thisflag;
        }
        mUhfrManager.stopTagInventory();
        //关闭读取延时100ms;
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUhfrManager.close();
        mUhfrManager = null;

        if (hasElecEpc == null || hasElecEpc.equals("")) {
            sendMes(mHandler, "未读取到上电的标签");
            return thisflag;
        }
        sendMes(mHandler, "扫描已停止！");
        long endTime = System.currentTimeMillis();
        sendMes(mHandler, "共有" + list2.size() + "个上电标签,用时：" + ((float) (endTime - startTime) / 1000) + "秒");
        sendMes(mHandler, "2、开始操作PSAM获命令...");
        startTime = System.currentTimeMillis();
        //开始PSAM操作
        psam = new PsamCmdUtil();
        if (psam.openRfid() != null) {
            sendMes(mHandler, "打开PSAM模块成功");
        }
        Error err = new Error();
        byte[] result = null;
        //复位
        result = psam.resetCard(psamCard);
        if (result != null) {
            sendMes(mHandler, "复位PSAM卡：" + Tools.Bytes2HexString(result, result.length));
        } else {
            sendMes(mHandler, "复位PSAM卡失败");
            Util.play(3, 0);
            ;
            return false;
        }
        //用户验证
        boolean flag = psam.verifyUser(psamCard,
                Tools.HexString2Bytes("5053414D49303031"),
                Tools.HexString2Bytes("4D494D49535F5053414D5F55534552"), err);
        if (flag) {
            sendMes(mHandler, "用户验证成功：");
        } else {
            sendMes(mHandler, "用户验证失败，错误码：" + err.getErrCode());
            Util.play(3, 0);
            return false;
        }
        //获取RFID访问密码
        String RfidPwd = "";
        result = psam.getRFIDPassword(psamCard, Tools.HexString2Bytes(hasElecEpc), err);
        if (result != null) {
            sendMes(mHandler, "获取RFID密码成功：" + Tools.Bytes2HexString(result, result.length));
            RfidPwd = Tools.Bytes2HexString(result, result.length);
        } else {
            sendMes(mHandler, "获取RFID密码失败，错误码：" + err.getErrCode());
            Util.play(3, 0);
            return false;
        }
        //获取加密命令
        String cmd = "";
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String DateTimeStr=sdf.format(new Date());
        if (lock) {
            result = psam.genCloseElsCmd(1,
                    Tools.HexString2Bytes(hasElecEpc),
                    20000,
                    Tools.HexString2Bytes("31323334"),
                    Tools.HexString2Bytes("31323334"),
                    DateTimeStr,
                    err
            );
        } else {
            result = psam.genOpenElsCmd(1,
                    Tools.HexString2Bytes(hasElecEpc),
                    0,
                    Tools.HexString2Bytes("31323334"),
                    Tools.HexString2Bytes("31323334"),
                    DateTimeStr,
                    err
            );
        }
        if (result != null) {
            cmd = Tools.Bytes2HexString(result, result.length);
            endTime = System.currentTimeMillis();

            sendMes(mHandler, "获取指令成功：,用时：" + ((float) (endTime - startTime) / 1000) + "秒");

        } else {
            sendMes(mHandler, "获取指令失败，错误码：" + err.getErrCode());
            Util.play(3, 0);
            return false;
        }
        psam.closeRfid();
        sendMes(mHandler, "关闭PSAM卡");
        endTime = System.currentTimeMillis();
        sendMes(mHandler, "操作PSAM卡用时：" + ((float) (endTime - startTime) / 1000) + "秒");
        //开始写入指令
        sendMes(mHandler, "3、开始写入指令...");
        startTime = System.currentTimeMillis();
        sendMes(mHandler, "读取指令区:");
        mUhfrManager = UHFRManager.getIntance();
        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(10, 10);
        sendMes(mHandler, "设置读取功率：" + 10 + " 写入功率：" + 10);
        int mbank = 3;//user区
        int startaddr = 76;
        int datalen = "6618B05CF3E99DEC4EE0747D83890C40FA01075AE5976A32DD51".length() / 2;
        byte rdata[] = new byte[datalen];
        byte[] password = Tools.HexString2Bytes(RfidPwd);
        short timeout =500;//读写100ms
        sendMes(mHandler, "盘点过滤值:" + hasElecEpc.substring(0, 8));
        byte[] fdata = Tools.HexString2Bytes(hasElecEpc.substring(0, 8));
        int fbank = 1;
        int fstartaddr = 2;
        boolean matching = true;
        byte[] usercmddata = mUhfrManager.getTagDataByFilter(mbank, startaddr, datalen, password, timeout, fdata, fbank, fstartaddr, matching);
        if (usercmddata != null) {
            sendMes(mHandler, "指令区读取成功:" + Tools.Bytes2HexString(usercmddata, usercmddata.length));
        } else {
            sendMes(mHandler, "指令区读取失败!");
            Util.play(3, 0);
            return thisflag;
        }
        byte[] cmdB = Tools.HexString2Bytes("6618" + cmd);
        sendMes(mHandler, "写入指令区:" + ("6618" + cmd) + " 长度:" + cmdB.length);
        Reader.READER_ERR reader_err = mUhfrManager.writeTagDataByFilter((char) mbank, startaddr, cmdB, cmdB.length, password, timeout, fdata, fbank, fstartaddr, matching);
        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
            sendMes(mHandler, "指令区写入成功:");
            Util.play(2,0);
        } else {
            sendMes(mHandler, "指令区写入失败!");
            endTime = System.currentTimeMillis();
            sendMes(mHandler, "写入指令用时：" + ((float) (endTime - startTime) / 1000) + "秒");
            // runInventory() ;
            sendMes(mHandler, "*********************************************************");
            long allendTime = System.currentTimeMillis();
            sendMes(mHandler, "全部操作用时：" + ((float) (allendTime - allstartTime) / 1000) + "秒");
            Util.play(3, 0);
            return thisflag;
        }
//**********************************************************************test end
        //  mUhfrManager.setPower(10,10);
        //设置只盘点当前标签
        //
        //延时5秒盘点10次
        try {
            sendMes(mHandler, "延时2秒后读取结果....");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startTime = System.currentTimeMillis();
        sendMes(mHandler, "4、读取操作结果....");
        mUhfrManager.setInventoryFilter(fdata,fbank,fstartaddr,matching);
        List<Reader.TAGINFO> listrs;
        int scantimes = 0;
        while (scantimes < 50) {
            scantimes++;
            listrs = mUhfrManager.tagInventoryByTimer((short) 100);
            if (listrs != null && listrs.size() > 0) {
                sendMes(mHandler, "盘点到结果标签" + listrs.size() + "个");
                for (Reader.TAGINFO tfs : listrs) {
                    byte[] epcdata = tfs.EpcId;
                    String epcrs = Tools.Bytes2HexString(epcdata, epcdata.length);
                    sendMes(mHandler, "hasElecEpc=" + hasElecEpc);
                    sendMes(mHandler, "epcrs=" + epcrs);
                    if (hasElecEpc.substring(0, 8).equals(epcrs.substring(0, 8))) {
                        TagEpcData ted = EpcReader.readEpc(epcrs);
//                        sendMes(mHandler, "盘存到操作后标签：" + epcrs);
//                        sendMes(mHandler, "包号:" + ted.getTagid());
//                        sendMes(mHandler, "状态:" + (ted.getHasElec() ? "上电" : "未上电 ") + (ted.getLockstuts().equals("Lock") ? "关锁" : "开锁"));
                        String lockstatus = "";
                        if (lock) {
                            lockstatus = "Lock";

                        } else {
                            lockstatus = "unLock";
                        }
                        if (ted.getHasElec() == false && ted.getLockstuts().equals(lockstatus))//状态正常
                        {
                            sendMes(mHandler, "锁状态正常！");
                            thisflag = true;
                            endTime = System.currentTimeMillis();
                            sendMes(mHandler, "读取结果用时：" + ((float) (endTime - startTime) / 1000) + "秒");
                            // runInventory() ;
                            sendMes(mHandler, "*********************************************************");
                            long allendTime = System.currentTimeMillis();
                            sendMes(mHandler, "全部操作用时：" + ((float) (allendTime - allstartTime) / 1000) + "秒");
                            Util.play(2, 0);
                            mUhfrManager.stopTagInventory();
                            //关闭读取延时100ms;
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            mUhfrManager.close();
                            mUhfrManager = null;
                            return thisflag;
                        }
                    }
                }
                sendMes(mHandler, "第"+scantimes+"次未盘点到结果标签");
                thisflag = false;
            } else {
                sendMes(mHandler, "第"+scantimes+"次未盘点到结果标签");
                thisflag = false;
            }
        }
        sendMes(mHandler, "读取到操作结果：操作失败");
        thisflag = false;
    //    mUhfrManager.stopTagInventory();
        //关闭读取延时100ms;
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUhfrManager.close();
        mUhfrManager = null;

        return thisflag;
    }
    private boolean operateLockWsGetrs(Handler mHandler, boolean lock)
    {
        boolean thisflag = false;
        long startTime = System.currentTimeMillis();
        long allstartTime = System.currentTimeMillis();
        sendMes(mHandler, "*********************************************************");
        sendMes(mHandler, "1、开始扫描标签...");
        mUhfrManager = UHFRManager.getIntance();

        //******************************************test start
        mUhfrManager.setPower(5, 5);
        List<Reader.TAGINFO> list1;
        list1 = mUhfrManager.tagInventoryByTimer((short) 50);

        String hasElecEpc = "";
        List<Reader.TAGINFO> list2;
        if (list1 != null && list1.size() > 0) {//

            list2 = new ArrayList<Reader.TAGINFO>();
            for (Reader.TAGINFO tfs : list1) {
                byte[] epcdata = tfs.EpcId;
                epc = Tools.Bytes2HexString(epcdata, epcdata.length);
                TagEpcData ted = EpcReader.readEpc(epc);
                if (ted != null) {
                    // if(ted.getHasElec())//如果是上电标签
                    //     if (ted.getTagid() > 1000000000L&&ted.getHasElec())//如果存在大于0的包号
                    if (ted.getTagid() > 1L&&ted.getHasElec())
                    {
                        list2.add(tfs);
                        hasElecEpc = epc;
                        int rssi = tfs.RSSI;
                        sendMes(mHandler, "epc=" + epc);
                        Message msg = new Message();
                        msg.what = 1;
                        Bundle b = new Bundle();
                        b.putString("epc", epc);
                        b.putString("rssi", rssi + "");
                        msg.setData(b);
                        mHandler.sendMessage(msg);
                    }
                }
            }
        } else {
            sendMes(mHandler, "未读取到标签");
            Util.play(3, 0);
            return thisflag;
        }
        mUhfrManager.stopTagInventory();
        //关闭读取延时100ms;
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUhfrManager.close();
        mUhfrManager = null;

        if (hasElecEpc == null || hasElecEpc.equals("")) {
            sendMes(mHandler, "未读取到上电的标签");
            return thisflag;
        }
        sendMes(mHandler, "扫描已停止！");
        long endTime = System.currentTimeMillis();
        sendMes(mHandler, "共有" + list2.size() + "个上电标签,用时：" + ((float) (endTime - startTime) / 1000) + "秒");
        sendMes(mHandler, "2、开始操作PSAM获命令...");
        startTime = System.currentTimeMillis();
        //开始PSAM操作
        psam = new PsamCmdUtil();
        if (psam.openRfid() != null) {
            sendMes(mHandler, "打开PSAM模块成功");
        }
        Error err = new Error();
        byte[] result = null;
        //复位
        result = psam.resetCard(psamCard);
        if (result != null) {
            sendMes(mHandler, "复位PSAM卡：" + Tools.Bytes2HexString(result, result.length));
        } else {
            sendMes(mHandler, "复位PSAM卡失败");
            Util.play(3, 0);
            ;
            return false;
        }
        //用户验证
        boolean flag = psam.verifyUser(psamCard,
                Tools.HexString2Bytes("5053414D49303031"),
                Tools.HexString2Bytes("4D494D49535F5053414D5F55534552"), err);
        if (flag) {
            sendMes(mHandler, "用户验证成功：");
        } else {
            sendMes(mHandler, "用户验证失败，错误码：" + err.getErrCode());
            Util.play(3, 0);
            return false;
        }
        //获取RFID访问密码
        String RfidPwd = "";
        result = psam.getRFIDPassword(psamCard, Tools.HexString2Bytes(hasElecEpc), err);
        if (result != null) {
            sendMes(mHandler, "获取RFID密码成功：" + Tools.Bytes2HexString(result, result.length));
            RfidPwd = Tools.Bytes2HexString(result, result.length);
        } else {
            sendMes(mHandler, "获取RFID密码失败，错误码：" + err.getErrCode());
            Util.play(3, 0);
            return false;
        }
        //获取加密命令
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String DateTimeStr=sdf.format(new Date());
        String cmd = "";
        if (lock) {
            result = psam.genCloseWriteElsCmd(1,
                    Tools.HexString2Bytes(hasElecEpc),
                    20000,
                    Tools.HexString2Bytes("31323334"),
                    Tools.HexString2Bytes("31323334"),
                    DateTimeStr,
                    err
            );
        } else {
            result = psam.genOpenWriteElsCmd(1,
                    Tools.HexString2Bytes(hasElecEpc),
                    0,
                    Tools.HexString2Bytes("31323334"),
                    Tools.HexString2Bytes("31323334"),
                    DateTimeStr,
                    err
            );
        }
        if (result != null) {
            cmd = Tools.Bytes2HexString(result, result.length);
            endTime = System.currentTimeMillis();

            sendMes(mHandler, "获取指令成功：,用时：" + ((float) (endTime - startTime) / 1000) + "秒");

        } else {
            sendMes(mHandler, "获取指令失败，错误码：" + err.getErrCode());
            Util.play(3, 0);
            return false;
        }
        psam.closeRfid();
        sendMes(mHandler, "关闭PSAM卡");
        endTime = System.currentTimeMillis();
        sendMes(mHandler, "操作PSAM卡用时：" + ((float) (endTime - startTime) / 1000) + "秒");
        //开始写入指令
        sendMes(mHandler, "3、开始写入指令...");
        startTime = System.currentTimeMillis();
        sendMes(mHandler, "读取指令区:");
        mUhfrManager = UHFRManager.getIntance();
        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(10, 10);
        sendMes(mHandler, "设置读取功率：" + 10 + " 写入功率：" + 10);
        int mbank = 3;//user区
        int startaddr = 76;
        int datalen = "6618B05CF3E99DEC4EE0747D83890C40FA01075AE5976A32DD51".length() / 2;
        byte rdata[] = new byte[datalen];
        byte[] password = Tools.HexString2Bytes(RfidPwd);
        short timeout =500;//读写100ms
        sendMes(mHandler, "盘点过滤值:" + hasElecEpc.substring(0, 8));
        byte[] fdata = Tools.HexString2Bytes(hasElecEpc.substring(0, 8));
        int fbank = 1;
        int fstartaddr = 2;
        boolean matching = true;
        byte[] usercmddata = mUhfrManager.getTagDataByFilter(mbank, startaddr, datalen, password, timeout, fdata, fbank, fstartaddr, matching);
        if (usercmddata != null) {
            sendMes(mHandler, "指令区读取成功:" + Tools.Bytes2HexString(usercmddata, usercmddata.length));
        } else {
            sendMes(mHandler, "指令区读取失败!");
            Util.play(3, 0);
            return thisflag;
        }
        byte[] cmdB = Tools.HexString2Bytes("6618" + cmd);
        sendMes(mHandler, "写入指令区:" + ("6618" + cmd) + " 长度:" + cmdB.length);
        Reader.READER_ERR reader_err = mUhfrManager.writeTagDataByFilter((char) mbank, startaddr, cmdB, cmdB.length, password, timeout, fdata, fbank, fstartaddr, matching);
        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
            sendMes(mHandler, "指令区写入成功:");
            Util.play(2,0);
        } else {
            sendMes(mHandler, "指令区写入失败!");
            endTime = System.currentTimeMillis();
            sendMes(mHandler, "写入指令用时：" + ((float) (endTime - startTime) / 1000) + "秒");
            // runInventory() ;
            sendMes(mHandler, "*********************************************************");
            long allendTime = System.currentTimeMillis();
            sendMes(mHandler, "全部操作用时：" + ((float) (allendTime - allstartTime) / 1000) + "秒");
            Util.play(3, 0);
            return thisflag;
        }
//**********************************************************************test end
        //  mUhfrManager.setPower(10,10);
        //设置只盘点当前标签
        //
        //延时5秒盘点10次
        try {
            sendMes(mHandler, "延时2秒后读取结果....");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startTime = System.currentTimeMillis();
        sendMes(mHandler, "4、读取操作结果....");
        mUhfrManager.setInventoryFilter(fdata,fbank,fstartaddr,matching);
        List<Reader.TAGINFO> listrs;
        int scantimes = 0;
        while (scantimes < 50) {
            scantimes++;
            listrs = mUhfrManager.tagInventoryByTimer((short) 100);
            if (listrs != null && listrs.size() > 0) {
                sendMes(mHandler, "盘点到结果标签" + listrs.size() + "个");
                for (Reader.TAGINFO tfs : listrs) {
                    byte[] epcdata = tfs.EpcId;
                    String epcrs = Tools.Bytes2HexString(epcdata, epcdata.length);
                    sendMes(mHandler, "hasElecEpc=" + hasElecEpc);
                    sendMes(mHandler, "epcrs=" + epcrs);
                    if (hasElecEpc.substring(0, 8).equals(epcrs.substring(0, 8))) {
                        TagEpcData ted = EpcReader.readEpc(epcrs);
//                        sendMes(mHandler, "盘存到操作后标签：" + epcrs);
//                        sendMes(mHandler, "包号:" + ted.getTagid());
//                        sendMes(mHandler, "状态:" + (ted.getHasElec() ? "上电" : "未上电 ") + (ted.getLockstuts().equals("Lock") ? "关锁" : "开锁"));
                        String lockstatus = "";
                        if (lock) {
                            lockstatus = "Lock";

                        } else {
                            lockstatus = "unLock";
                        }
                        if (ted.getHasElec() == false && ted.getLockstuts().equals(lockstatus))//状态正常
                        {
                            sendMes(mHandler, "锁状态正常！");
                            thisflag = true;
                            endTime = System.currentTimeMillis();
                            sendMes(mHandler, "读取结果用时：" + ((float) (endTime - startTime) / 1000) + "秒");
                            // runInventory() ;
                            sendMes(mHandler, "*********************************************************");
                            long allendTime = System.currentTimeMillis();
                            sendMes(mHandler, "全部操作用时：" + ((float) (allendTime - allstartTime) / 1000) + "秒");
                            Util.play(2, 0);
                            mUhfrManager.stopTagInventory();
                            //关闭读取延时100ms;
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            mUhfrManager.close();
                            mUhfrManager = null;
                            return thisflag;
                        }
                    }
                }
                sendMes(mHandler, "第"+scantimes+"次未盘点到结果标签");
                thisflag = false;
            } else {
                sendMes(mHandler, "第"+scantimes+"次未盘点到结果标签");
                thisflag = false;
            }
        }
        sendMes(mHandler, "读取到操作结果：操作失败");
        thisflag = false;
        mUhfrManager.stopTagInventory();
        //关闭读取延时100ms;
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUhfrManager.close();
        mUhfrManager = null;

        return thisflag;
    }
    private boolean reSet(Handler mHandler) {
        return  false;
    }
    private boolean operateLock(Handler mHandler, boolean lock) {
        boolean thisflag = false;
        long startTime = System.currentTimeMillis();
        long allstartTime = System.currentTimeMillis();

        //  mSerialPort.switch2channel(13);
        //  Log.e(logtag,"串口切换到UHF:13");

        sendMes(mHandler, "*********************************************************");
        sendMes(mHandler, "1、开始扫描标签...");
        mUhfrManager = UHFRManager.getIntance();

        mUhfrManager.setCancleFastMode();
        //******************************************test start
        mUhfrManager.setPower(5, 5);
        List<Reader.TAGINFO> list1;
        list1 = mUhfrManager.tagInventoryByTimer((short) 50);

        String hasElecEpc = "";
        List<Reader.TAGINFO> list2;
        if (list1 != null && list1.size() > 0) {//

            list2 = new ArrayList<Reader.TAGINFO>();
            for (Reader.TAGINFO tfs : list1) {
                byte[] epcdata = tfs.EpcId;
                epc = Tools.Bytes2HexString(epcdata, epcdata.length);
                TagEpcData ted = EpcReader.readEpc(epc);
                if (ted != null) {
                    // if(ted.getHasElec())//如果是上电标签
                    if (ted.getTagid() > 1000000000L&&ted.getHasElec())//如果存在大于0的包号
                    {
                        list2.add(tfs);
                        hasElecEpc = epc;
                        int rssi = tfs.RSSI;
                        sendMes(mHandler, "epc=" + epc);
                        Message msg = new Message();
                        msg.what = 1;
                        Bundle b = new Bundle();
                        b.putString("epc", epc);
                        b.putString("rssi", rssi + "");
                        msg.setData(b);
                        mHandler.sendMessage(msg);
                    }
                }
            }
        } else {
            sendMes(mHandler, "未读取到标签");
            Util.play(3, 0);
            return thisflag;
        }
        mUhfrManager.stopTagInventory();
        //关闭读取延时500ms;
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUhfrManager.close();
        mUhfrManager = null;

        if (hasElecEpc == null || hasElecEpc.equals("")) {
            sendMes(mHandler, "未读取到上电的标签");
            return thisflag;
        }


        sendMes(mHandler, "扫描已停止！");


        long endTime = System.currentTimeMillis();
        sendMes(mHandler, "共有" + list2.size() + "个上电标签,用时：" + ((float) (endTime - startTime) / 1000) + "秒");

        sendMes(mHandler, "2、开始操作PSAM获命令...");
        startTime = System.currentTimeMillis();
        //开始PSAM操作
        psam = new PsamCmdUtil();
        if (psam.openRfid() != null) {
            sendMes(mHandler, "打开PSAM模块成功");
        }
        Error err = new Error();
        byte[] result = null;
        //复位
        result = psam.resetCard(psamCard);
        if (result != null) {
            sendMes(mHandler, "复位PSAM卡：" + Tools.Bytes2HexString(result, result.length));
        } else {

            sendMes(mHandler, "复位PSAM卡失败");
            Util.play(3, 0);
            ;
            return false;
        }
        //用户验证
        boolean flag = psam.verifyUser(psamCard,
                Tools.HexString2Bytes("5053414D49303031"),
                Tools.HexString2Bytes("4D494D49535F5053414D5F55534552"), err);
        if (flag) {

            sendMes(mHandler, "用户验证成功：");
        } else {

            sendMes(mHandler, "用户验证失败，错误码：" + err.getErrCode());
            Util.play(3, 0);
            return false;
        }
        //获取RFID访问密码
        String RfidPwd = "";
        result = psam.getRFIDPassword(psamCard, Tools.HexString2Bytes(hasElecEpc), err);
        if (result != null) {

            sendMes(mHandler, "获取RFID密码成功：" + Tools.Bytes2HexString(result, result.length));
            RfidPwd = Tools.Bytes2HexString(result, result.length);
        } else {

            sendMes(mHandler, "获取RFID密码失败，错误码：" + err.getErrCode());
            Util.play(3, 0);
            return false;
        }
        //获取加密命令
        String cmd = "";
        if (lock) {
            result = psam.genCloseElsCmd(1,
                    Tools.HexString2Bytes(hasElecEpc),
                    20000,
                    Tools.HexString2Bytes("31323334"),
                    Tools.HexString2Bytes("31323334"),
                    "2019-4-4 16:52:21",
                    err
            );
        } else {
            result = psam.genOpenElsCmd(1,
                    Tools.HexString2Bytes(hasElecEpc),
                    0,
                    Tools.HexString2Bytes("31323334"),
                    Tools.HexString2Bytes("31323334"),
                    "2019-4-4 16:52:21",
                    err
            );
        }
        if (result != null) {
            cmd = Tools.Bytes2HexString(result, result.length);
            endTime = System.currentTimeMillis();

            sendMes(mHandler, "获取指令成功：,用时：" + ((float) (endTime - startTime) / 1000) + "秒");

        } else {

            sendMes(mHandler, "获取指令失败，错误码：" + err.getErrCode());
            Util.play(3, 0);
            return false;
        }
        psam.closeRfid();
        sendMes(mHandler, "关闭PSAM卡");
        endTime = System.currentTimeMillis();
        sendMes(mHandler, "操作PSAM卡用时：" + ((float) (endTime - startTime) / 1000) + "秒");
        //开始写入指令

        sendMes(mHandler, "3、开始写入指令...");

        startTime = System.currentTimeMillis();
        sendMes(mHandler, "读取指令区:");
        mUhfrManager = UHFRManager.getIntance();
        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(5, 10);
        sendMes(mHandler, "设置读取功率：" + 5 + " 写入功率：" + 10);
        int mbank = 3;//user区
        int startaddr = 76;
        int datalen = "6618B05CF3E99DEC4EE0747D83890C40FA01075AE5976A32DD51".length() / 2;
        byte rdata[] = new byte[datalen];
        byte[] password = Tools.HexString2Bytes(RfidPwd);
        short timeout = 500;

        sendMes(mHandler, "盘点过滤值:" + hasElecEpc.substring(0, 8));
        byte[] fdata = Tools.HexString2Bytes(hasElecEpc.substring(0, 8));
        int fbank = 1;
        int fstartaddr = 2;
        boolean matching = true;



        byte[] usercmddata = mUhfrManager.getTagDataByFilter(mbank, startaddr, datalen, password, timeout, fdata, fbank, fstartaddr, matching);
        if (usercmddata != null) {
            sendMes(mHandler, "指令区读取成功:" + Tools.Bytes2HexString(usercmddata, usercmddata.length));
        } else {
            sendMes(mHandler, "指令区读取失败!");
            Util.play(3, 0);
            return thisflag;
        }

        byte[] cmdB = Tools.HexString2Bytes("6618" + cmd);
        sendMes(mHandler, "写入指令区:" + ("6618" + cmd) + " 长度:" + cmdB.length);


        Reader.READER_ERR reader_err = mUhfrManager.writeTagDataByFilter((char) mbank, startaddr, cmdB, cmdB.length, password, timeout, fdata, fbank, fstartaddr, matching);
        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
            sendMes(mHandler, "指令区写入成功:");
            Util.play(2,0);
        } else {
            sendMes(mHandler, "指令区写入失败!");
            endTime = System.currentTimeMillis();
            sendMes(mHandler, "写入指令用时：" + ((float) (endTime - startTime) / 1000) + "秒");
            // runInventory() ;
            sendMes(mHandler, "*********************************************************");
            long allendTime = System.currentTimeMillis();
            sendMes(mHandler, "全部操作用时：" + ((float) (allendTime - allstartTime) / 1000) + "秒");
            Util.play(3, 0);
            return thisflag;
        }
        endTime = System.currentTimeMillis();
        sendMes(mHandler, "写入指令用时：" + ((float) (endTime - startTime) / 1000) + "秒");
        // runInventory() ;
        sendMes(mHandler, "*********************************************************");


        long allendTime = System.currentTimeMillis();
        sendMes(mHandler, "全部操作用时：" + ((float) (allendTime - allstartTime) / 1000) + "秒");
//**********************************************************************test end
        //  mUhfrManager.setPower(10,10);
        //设置只盘点当前标签

        //
        //延时5秒盘点10次
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        mUhfrManager.setInventoryFilter(fdata,fbank,fstartaddr,matching);
//        List<Reader.TAGINFO> listrs;
//        int scantimes = 0;
//        while (scantimes < 10) {
//            scantimes++;
//            listrs = mUhfrManager.tagInventoryByTimer((short) 1000);
//            if (listrs != null && listrs.size() > 0) {
//                sendMes(mHandler, "盘点到结果标签" + listrs.size() + "次");
//                for (Reader.TAGINFO tfs : listrs) {
//                    byte[] epcdata = tfs.EpcId;
//                    String epcrs = Tools.Bytes2HexString(epcdata, epcdata.length);
//                    sendMes(mHandler, "hasElecEpc=" + hasElecEpc);
//                    sendMes(mHandler, "epcrs=" + epcrs);
//                    if (hasElecEpc.substring(0, 8).equals(epcrs.substring(0, 8))) {
//                        TagEpcData ted = EpcReader.readEpc(epcrs);
//                        sendMes(mHandler, "盘存到操作后标签：" + epcrs);
//                        sendMes(mHandler, "包号:" + ted.getTagid());
//                        sendMes(mHandler, "状态:" + (ted.getHasElec() ? "上电" : "未上电 ") + (ted.getLockstuts().equals("Lock") ? "关锁" : "开锁"));
//                        String lockstatus = "";
//                        if (lock) {
//                            lockstatus = "Lock";
//
//                        } else {
//                            lockstatus = "unLock";
//                        }
//                        if (ted.getHasElec() == false && ted.getLockstuts().equals(lockstatus))//状态正常
//                        {
//                            sendMes(mHandler, "锁状态正常！");
//                            thisflag = true;
//                            Util.play(2, 0);
//                            return thisflag;
//                        }
//                    }
//                }
//                sendMes(mHandler, "未盘点到结果标签");
//                thisflag = false;
//            } else {
//                sendMes(mHandler, "未盘点到结果标签");
//                thisflag = false;
//            }
//        }
//
//        mUhfrManager.stopTagInventory();
//        //关闭读取延时500ms;
//        try {
//            Thread.sleep(500);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        mUhfrManager.close();
//        mUhfrManager = null;

        return thisflag;
    }

    private boolean isRunning = false;
    private boolean isStart = false;

    //inventory epc
    private Runnable inventoryTask = new Runnable() {
        @Override
        public void run() {
            while (isRunning) {
                if (isStart) {
                    List<Reader.TAGINFO> list1;
                    if (isMulti) { // multi mode
                        list1 = mUhfrManager.tagInventoryRealTime();
                    } else {
                        //sleep can save electricity
//						try {
//							Thread.sleep(250);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
                        list1 = mUhfrManager.tagInventoryByTimer((short) 50);
                        //inventory epc + tid
//						list1 = MainActivity.mUhfrManager.tagEpcTidInventoryByTimer((short) 50);
                    }//
                    if (list1 != null && list1.size() > 0) {//
                        for (Reader.TAGINFO tfs : list1) {
                            byte[] epcdata = tfs.EpcId;
                            epc = Tools.Bytes2HexString(epcdata, epcdata.length);
                            int rssi = tfs.RSSI;

                            Message msg = new Message();
                            msg.what = 1;
                            Bundle b = new Bundle();
                            b.putString("epc", epc);
                            b.putString("rssi", rssi + "");
                            msg.setData(b);
                            mHandler.sendMessage(msg);
                        }
                    }
                    //inventory epc + tid
//                    if (list1 != null && list1.size() > 0) {
//                        for (TAGINFO tfs : list1) {
//                            byte[] epcdata = tfs.EpcId;
//                            epc = Tools.Bytes2HexString(epcdata, epcdata.length);
//                            int rssi = tfs.RSSI;
//                            String tid = Tools.Bytes2HexString(tfs.EmbededData, tfs.EmbededDatalen);
//                            Log.e("Huang, Fragment1", "tid = " + tid);
//                            Message msg = new Message();
//                            msg.what = 1;
//                            Bundle b = new Bundle();
//                            b.putString("epc", epc);
//                            b.putString("rssi", rssi + "");
//                            msg.setData(b);
//                            handler.sendMessage(msg);
//                        }
//                    }
                }
            }
        }
    };
    private boolean keyControl = true;

    private void runInventory() {
        if (keyControl) {
            keyControl = false;
            if (!isStart) {
                mUhfrManager.setCancleInventoryFilter();
                isRunning = true;
                if (isMulti) {
                    mUhfrManager.setFastMode();
                    mUhfrManager.asyncStartReading();
                } else {
                    mUhfrManager.setCancleFastMode();
                }
                new Thread(inventoryTask).start();

//            Log.e("inventoryTask", "start inventory") ;
                isStart = true;
            } else {
//                checkMulti.setClickable(true);
//                checkMulti.setTextColor(Color.BLACK);
                if (isMulti)
                    mUhfrManager.asyncStopReading();
                else
                    mUhfrManager.stopTagInventory();
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                isRunning = false;
                //btnStart.setText(getResources().getString(R.string.start_inventory_epc));
                isStart = false;
            }
            keyControl = true;
        }
    }

    private void setlog(String log) {
        tvinfo.setText(tvinfo.getText() + log + "\n");
    }

    private void sendMes(Handler mHandler, String mes) {
        Log.e(logtag, mes);
        Message message = new Message();
        message.what = INFO_MES;
        Bundle data = new Bundle();
        data.putString("message", mes);
        message.setData(data);
        mHandler.sendMessage(message);
    }
}
