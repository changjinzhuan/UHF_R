package com.handheld.uhfrdemo.cn.kcrxorg.uhfutil;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.BRMicro.Tools;
import com.handheld.uhfr.UHFRManager;
import com.handheld.uhfrdemo.Util;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.Error;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.PsamCmdUtil;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.cn.kcrx.bean.TagEpcData;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.cn.kcrx.bean.TagUserdata;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.rfidtool.EpcReader;
import com.uhf.api.cls.Reader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LockHelper {
    static String logtag = "kcrxlog";

    private static PsamCmdUtil psam;

    private static int psamCard = 1;//PSAM卡座号

    public static UHFRManager mUhfrManager;//uhf
    private static final int INFO_MES = 0;

    private Handler mHandler;

    public void changePort(int times) {
        sendMes("测试开始......");
        for (int i = 0; i < times; i++) {
            //打开天线...

            mUhfrManager = UHFRManager.getIntance();
            mUhfrManager.setCancleFastMode();
            mUhfrManager.setPower(30, 10);
            sendMes("打开天线模块成功");
            List<Reader.TAGINFO> taginfos = mUhfrManager.tagInventoryByTimer((short) 100);

            for (Reader.TAGINFO t : taginfos) {
                sendMes("读取到标签：" + Tools.Bytes2HexString(t.EpcId, t.EpcId.length));
            }
            try {
                sendMes("等待10秒");
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //关闭天线
            mUhfrManager.stopTagInventory();
            //关闭读取延时200ms;
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mUhfrManager.close();
            mUhfrManager = null;
            sendMes("关闭天线模块成功");
            //打开PSAM
            psam = new PsamCmdUtil();
            if (psam.openRfid() != null) {
                sendMes("打开PSAM模块成功");
            }
            Error err = new Error();
            byte[] result = null;
            //复位
            result = psam.resetCard(psamCard);
            if (result != null) {
                sendMes("复位PSAM卡：" + Tools.Bytes2HexString(result, result.length));
            } else {
                sendMes("复位PSAM卡失败");
            }
            //
            try {
                sendMes("等待10秒");
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //关闭PSAM
            psam.closeRfid();
            sendMes("关闭PSAM卡");
        }
        sendMes("测试完成......");
    }

    public LOCK_ERR Lockws(String operator1str, String operator2str, boolean lock) {

        //启动天线读取
        mUhfrManager = UHFRManager.getIntance();

        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(10, 10);
        List<Reader.TAGINFO> list1;
        list1 = mUhfrManager.tagInventoryByTimer((short) 100);
        //过滤非法及未上电标签
        String epc = "";
        String hasElecEpc = "";
        List<Reader.TAGINFO> list2;

        if (list1 != null && list1.size() > 0) {
            list2 = new ArrayList<Reader.TAGINFO>();
            for (Reader.TAGINFO tfs : list1) {
                byte[] epcdata = tfs.EpcId;
                epc = Tools.Bytes2HexString(epcdata, epcdata.length);

                TagEpcData ted = EpcReader.readEpc(epc);
                sendMes("读取到EPC" + epc);
                sendMes("包号:" + ted.getTagid() + " 上电状态:" + ted.getHasElec() + " 锁状态:" + ted.getLockstuts() + " 异常标志:" + ted.getLockeEx() + " EPC异常:" + ted.getEpcEx() + " 工作状态:" + ted.getJobstuts());
                if (ted != null) {
                    if (ted.getTagid() > 1000000000L && ted.getHasElec())//如果存在大于0的包号并且已经上电
                    {
                        list2.add(tfs);
                        hasElecEpc = epc;
                    }
                }
            }
        } else {
            return LOCK_ERR.LOCK_NOEPC_ERR;
        }
        //查找信号最强的标签
        Reader.TAGINFO Maxtfs = null;
        if (list2 != null && list2.size() > 0) {
            int rssi = list2.get(0).RSSI;
            int maxnum = 0;
            for (int i = 0; i < list2.size(); i++) {
                if (list2.get(i).RSSI > rssi) {
                    rssi = list2.get(i).RSSI;
                    maxnum = i;
                }
            }
            Maxtfs = list2.get(maxnum);
        } else {
            return LOCK_ERR.LOCK_NOEPC_ERR;
        }
        byte[] Maxepcdata = Maxtfs.EpcId;
        String Maxepc = Tools.Bytes2HexString(Maxepcdata, Maxepcdata.length);
        sendMes("获取到最佳信号标签为:" + Maxepc + " 信号:" + Maxtfs.RSSI);
        sendMes("包号" + EpcReader.readEpc(Maxepc).getTagid());
        //关闭UHF天线
        mUhfrManager.stopTagInventory();
        //关闭读取延时200ms;
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUhfrManager.close();
        mUhfrManager = null;

        //打开PSAM卡
        psam = new PsamCmdUtil();
        if (psam.openRfid() != null) {
            sendMes("打开PSAM模块成功");
        }
        Error err = new Error();
        byte[] result = null;
        //复位
        result = psam.resetCard(psamCard);
        if (result != null) {
            sendMes("复位PSAM卡：" + Tools.Bytes2HexString(result, result.length));
        } else {
            sendMes("复位PSAM卡失败");
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //用户验证
        boolean flag = psam.verifyUser(psamCard,
                Tools.HexString2Bytes("5053414D49303031"),
                Tools.HexString2Bytes("4D494D49535F5053414D5F55534552"), err);
        if (flag) {
            sendMes("用户验证成功：");
        } else {
            sendMes("用户验证失败，错误码：" + err.getErrCode());
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //获取RFID访问密码
        String RfidPwd = "";
        result = psam.getRFIDPassword(psamCard, Tools.HexString2Bytes(Maxepc), err);
        if (result != null) {
            sendMes("获取RFID密码成功：" + Tools.Bytes2HexString(result, result.length));
            RfidPwd = Tools.Bytes2HexString(result, result.length);
        } else {

            sendMes("获取RFID密码失败，错误码：" + err.getErrCode());
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //获取加密命令
        String cmd = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String DateTimeStr = sdf.format(new Date());
        if (lock) {
            result = psam.genCloseElsCmd(1,
                    Tools.HexString2Bytes(Maxepc),
                    20000,
                    Tools.HexString2Bytes(operator1str),
                    Tools.HexString2Bytes(operator2str),
                    DateTimeStr,
                    err
            );
        } else {
            result = psam.genOpenElsCmd(1,
                    Tools.HexString2Bytes(Maxepc),
                    0,
                    Tools.HexString2Bytes(operator1str),
                    Tools.HexString2Bytes(operator2str),
                    DateTimeStr,
                    err
            );
        }
        if (result != null) {
            cmd = Tools.Bytes2HexString(result, result.length);
            sendMes("获取开锁指令成功：" + Tools.Bytes2HexString(result, result.length));
        } else {

            sendMes("获取指令失败，错误码：" + err.getErrCode());
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        psam.closeRfid();
        sendMes("关闭PSAM卡");

        //开始写入指令操作
        mUhfrManager = UHFRManager.getIntance();
        mUhfrManager.setCancleFastMode();

        int mbank = 3;//user区
        int startaddr = 76;
        int datalen = "6618B05CF3E99DEC4EE0747D83890C40FA01075AE5976A32DD51".length() / 2;
        byte rdata[] = new byte[datalen];
        byte[] password = Tools.HexString2Bytes(RfidPwd);
        short timeout = 500;
        //读取指令区判断是否可操作
        Reader.READER_ERR reader_err = mUhfrManager.getTagData(mbank, startaddr, datalen, rdata, password, timeout);
        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
            sendMes("指令区读取成功:" + Tools.Bytes2HexString(rdata, rdata.length));
        } else {
            sendMes("指令区读取失败!");
            return LOCK_ERR.LOCK_READUSER_ERR;
        }

        if (!Tools.Bytes2HexString(rdata, rdata.length).startsWith("BB"))//如果不是BB开头，不可以操作写入指令
        {
            sendMes("不是BB开头，不可以操作写入指令!");
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        }
        //指令区可操作，开始写入
        byte[] cmdB = Tools.HexString2Bytes("6618" + cmd);
        sendMes("写入指令区:" + ("6618" + cmd) + " 长度:" + cmdB.length);
        mUhfrManager.setPower(10, 10);
        sendMes("设置读取功率：" + 10 + " 写入功率：" + 10);
        reader_err = mUhfrManager.writeTagData((char) mbank, startaddr, cmdB, cmdB.length, password, timeout);
        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
            sendMes("指令区写入成功:");
        } else {
            sendMes("指令区写入失败!");
            return LOCK_ERR.LOCK_WRITEUSER_ERR;
        }
        //写入成功，开始判断操作结果

        mUhfrManager.setPower(30, 10);
        //设置只盘点当前标签
        byte[] fdata = Tools.HexString2Bytes(Maxepc.substring(0, 8));
        int fbank = 1;
        int fstartaddr = 0;
        boolean matching = true;
        //   mUhfrManager.setInventoryFilter(fdata,fbank,fstartaddr,matching);
        //盘点5秒钟

        List<Reader.TAGINFO> listrs;
        listrs = mUhfrManager.tagInventoryByTimer((short) 5000);
        if (listrs != null && listrs.size() > 0) {
            sendMes("盘点到结果标签" + listrs.size() + "次");
            for (Reader.TAGINFO tfs : listrs) {
                byte[] epcdata = tfs.EpcId;
                String epcrs = Tools.Bytes2HexString(epcdata, epcdata.length);

                sendMes("Maxepc=" + Maxepc);
                sendMes("epcrs=" + epcrs);
                if (Maxepc.substring(0, 8).equals(epcrs.substring(0, 8))) {
                    TagEpcData ted = EpcReader.readEpc(epcrs);
                    sendMes("盘存到操作后标签：" + epcrs);
                    sendMes("包号:" + ted.getTagid());
                    sendMes("状态:" + (ted.getHasElec() ? "上电" : "未上电 ") + (ted.getLockstuts().equals("Lock") ? "关锁" : "开锁"));
                    String lockstatus = "";
                    if (lock) {
                        lockstatus = "Lock";

                    } else {
                        lockstatus = "unLock";
                    }
                    if (ted.getHasElec() == false && ted.getLockstuts().equals(lockstatus))//状态正常
                    {
                        sendMes("锁状态正常！");
                        Util.play(2, 0);
                        return LOCK_ERR.LOCK_OK;
                    }
                }


            }
        } else {
            sendMes("未盘点到结果标签");
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        }
        return LOCK_ERR.LOCK_LOCKRRS_ERR;
    }

    public LockHelper(Handler Handler) {
        mHandler = Handler;
    }

    public LOCK_ERR RFID_Active(byte Volume) {
        //启动天线读取
        mUhfrManager = UHFRManager.getIntance();
        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(10, 25);
        List<Reader.TAGINFO> list1;
        list1 = mUhfrManager.tagInventoryByTimer((short) 100);
        //过滤非法及未上电标签
        String epc = "";
        String hasElecEpc = "";
        List<Reader.TAGINFO> list2;

        if (list1 != null && list1.size() > 0) {
            list2 = new ArrayList<Reader.TAGINFO>();
            for (Reader.TAGINFO tfs : list1) {
                byte[] epcdata = tfs.EpcId;
                epc = Tools.Bytes2HexString(epcdata, epcdata.length);

                TagEpcData ted = EpcReader.readEpc(epc);
                sendMes("读取到EPC" + epc);

                if (ted != null) {
                    sendMes("包号:" + ted.getTagid() + " 上电状态:" + ted.getHasElec() + " 券别ID=" + ted.getPervalueid());
                    if (ted.getTagid() > 1000000000L && ted.getHasElec() && ted.getPervalueid() == 0)//如果存在大于0的包号并且已经上电并且券别不应是0才可以激活
                    // if(ted.getTagid()>1000000000L&&ted.getHasElec())
                    {
                        list2.add(tfs);
                        hasElecEpc = epc;
                    }
                }
            }
        } else {
            return LOCK_ERR.LOCK_NOEPC_ERR;
        }
        //查找信号最强的标签
        Reader.TAGINFO Maxtfs = null;
        if (list2 != null && list2.size() > 0) {
            int rssi = list2.get(0).RSSI;
            int maxnum = 0;
            for (int i = 0; i < list2.size(); i++) {
                if (list2.get(i).RSSI > rssi) {
                    rssi = list2.get(i).RSSI;
                    maxnum = i;
                }
            }
            Maxtfs = list2.get(maxnum);
        } else {
            return LOCK_ERR.LOCK_NOEPC_ERR;
        }
        byte[] Maxepcdata = Maxtfs.EpcId;
        String Maxepc = Tools.Bytes2HexString(Maxepcdata, Maxepcdata.length);
        sendMes("获取到最佳信号标签为:" + Maxepc + " 信号:" + Maxtfs.RSSI);
        sendMes("包号" + EpcReader.readEpc(Maxepc).getTagid());
        //关闭UHF天线
        mUhfrManager.stopTagInventory();
        //关闭读取延时200ms;
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUhfrManager.close();
        mUhfrManager = null;

        //打开PSAM卡
        psam = new PsamCmdUtil();
        if (psam.openRfid() != null) {
            sendMes("打开PSAM模块成功");
        }
        Error err = new Error();
        byte[] result = null;
        //复位
        result = psam.resetCard(psamCard);
        if (result != null) {
            sendMes("复位PSAM卡：" + Tools.Bytes2HexString(result, result.length));
        } else {
            sendMes("复位PSAM卡失败");
            psam.closeRfid();
            sendMes("关闭PSAM卡");
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //用户验证
        boolean flag = psam.verifyUser(psamCard,
                Tools.HexString2Bytes("5053414D49303031"),
                Tools.HexString2Bytes("4D494D49535F5053414D5F55534552"), err);
        if (flag) {
            sendMes("用户验证成功：");
        } else {
            psam.closeRfid();
            sendMes("关闭PSAM卡");
            sendMes("用户验证失败，错误码：" + err.getErrCode());
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //获取RFID访问密码
        String RfidPwd = "";
        result = psam.getRFIDPassword(psamCard, Tools.HexString2Bytes(Maxepc), err);
        if (result != null) {
            sendMes("获取RFID密码成功：" + Tools.Bytes2HexString(result, result.length));
            RfidPwd = Tools.Bytes2HexString(result, result.length);
        } else {
            psam.closeRfid();
            sendMes("关闭PSAM卡");
            sendMes("获取RFID密码失败，错误码：" + err.getErrCode());
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //获取激活加密命令
        String cmd = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String DateTimeStr = sdf.format(new Date());

        result = psam.genTagActiveElsCmd(1,
                Tools.HexString2Bytes(Maxepc),
                Volume,
                err);


        if (result != null) {
            cmd = Tools.Bytes2HexString(result, result.length);
            sendMes("获取激活指令成功：" + Tools.Bytes2HexString(result, result.length));
        } else {
            psam.closeRfid();
            sendMes("关闭PSAM卡");
            sendMes("获取激活指令失败，错误码：" + err.getErrCode());
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        psam.closeRfid();
        sendMes("关闭PSAM卡");

        //开始写入指令操作
        mUhfrManager = UHFRManager.getIntance();
        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(15, 25);
        sendMes("设置读取功率：" + 15 + " 写入功率：" + 25);

        int mbank = 3;//user区
        int startaddr = 76;
        int datalen = "6618B05CF3E99DEC4EE0747D83890C40FA01075AE5976A32DD51".length() / 2;
        byte rdata[] = new byte[datalen];
        byte[] password = Tools.HexString2Bytes(RfidPwd);
        short timeout = 500;
        byte[] fdata = Tools.HexString2Bytes(Maxepc.substring(0, 8));
        sendMes("盘点过滤值:" + Maxepc.substring(0, 8));
        int fbank = 1;
        int fstartaddr = 2;
        boolean matching = true;
        //读取指令区判断是否可操作
        byte[] usercmddata = mUhfrManager.getTagDataByFilter(mbank, startaddr, datalen, password, timeout, fdata, fbank, fstartaddr, matching);
        if (usercmddata != null) {
            sendMes("指令区读取成功:" + Tools.Bytes2HexString(usercmddata, usercmddata.length));
        } else {
            sendMes("指令区读取失败!");
            Util.play(3, 0);
            return LOCK_ERR.LOCK_READUSER_ERR;
        }
        if (!Tools.Bytes2HexString(usercmddata, usercmddata.length).startsWith("BB"))//如果不是BB开头，不可以操作写入指令
        {
            sendMes("不是BB开头，不可以操作写入指令!");
            closeUHF();
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        }
//        Reader.READER_ERR reader_err = mUhfrManager.getTagData(mbank, startaddr, datalen, rdata, password, timeout);
//        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
//            sendMes("指令区读取成功:" + Tools.Bytes2HexString(rdata, rdata.length));
//        } else {
//            sendMes("指令区读取失败!");
//            //关闭UHF天线
//            mUhfrManager.stopTagInventory();
//            //关闭读取延时200ms;
//            try {
//                Thread.sleep(200);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            mUhfrManager.close();
//            mUhfrManager = null;
//            return LOCK_ERR.LOCK_READUSER_ERR;
//        }

//        if (!Tools.Bytes2HexString(rdata, rdata.length).startsWith("BB"))//如果不是BB开头，不可以操作写入指令
//        {
//            sendMes("不是BB开头，不可以操作写入指令!");
//            closeUHF();
//            return LOCK_ERR.LOCK_LOCKRRS_ERR;
//        }
        //指令区可操作，开始写入
        byte[] cmdB = Tools.HexString2Bytes("66" + intToHex(cmd.length() / 2) + cmd);
        sendMes("写入指令区:" + "66" + intToHex(cmd.length() / 2) + cmd + " 长度:" + cmdB.length);
      //  mUhfrManager.setPower(10, 10);
      //  sendMes("设置读取功率：" + 10 + " 写入功率：" + 10);
        Reader.READER_ERR reader_err = mUhfrManager.writeTagDataByFilter((char) mbank, startaddr, cmdB, cmdB.length, password, timeout, fdata, fbank, fstartaddr, matching);
        //  reader_err = mUhfrManager.writeTagData((char) mbank, startaddr, cmdB, cmdB.length, password, timeout);
        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
            sendMes("指令区写入成功！");
            //  closeUHF();
            //  return LOCK_ERR.LOCK_OK;
        } else {
            sendMes("指令区写入失败!");
            closeUHF();
            return LOCK_ERR.LOCK_WRITEUSER_ERR;
        }


        //写入成功，开始判断操作结果
        sendMes("等待8秒....");
        //写入成功后，等待2秒
        try {
            Thread.sleep(8000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//
//
//        mUhfrManager.setPower(30,10);
        //设置只盘点当前标签
//        sendMes("设定盘点值:" + Maxepc.substring(0, 8));
//        byte[] fdata = Tools.HexString2Bytes(Maxepc.substring(0, 8));
//        int fbank = 1;//EPC
//        int fstartaddr = 2;//偏移2
//        boolean matching = true;
        //byte[] usercmddata = mUhfrManager.getTagDataByFilter(mbank, startaddr, datalen, password, timeout, fdata, fbank, fstartaddr, matching);
        mUhfrManager.setInventoryFilter(fdata, fbank, fstartaddr, matching);
        //盘点5秒钟
//
        List<Reader.TAGINFO> listrs;
        listrs = mUhfrManager.tagInventoryByTimer((short) 100);
        if (listrs != null && listrs.size() > 0) {
            sendMes("盘点到结果标签" + listrs.size() + "次");
            for (Reader.TAGINFO tfs : listrs) {
                byte[] epcdata = tfs.EpcId;
                String epcrs = Tools.Bytes2HexString(epcdata, epcdata.length);
                TagEpcData ted = EpcReader.readEpc(epcrs);
                sendMes("盘存到操作后标签：" + epc);
                sendMes("包号:" + ted.getTagid());
                sendMes("锁状态:" + ted.getLockstuts());
                sendMes("激活后券别:" + ted.getPervalueid());
                String lockstatus = "";
                int intVolume = (int) Volume;
                sendMes("应激活券别:" + intVolume);
                // if(ted.getHasElec()==false&&intVolume==ted.getPervalueid())//激活正常
                if (ted.getHasElec() == true && intVolume == ted.getPervalueid())//激活正常
                {
                    sendMes("锁状态正常！");
                    closeUHF();
                    return LOCK_ERR.LOCK_OK;
                }
            }
            sendMes("未盘点正确结果标签，激活失败!");
            closeUHF();
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        } else {
            sendMes("未盘点到结果标签，激活失败!");
            closeUHF();
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        }


    }

    public void closeUHF() {
        if (mUhfrManager != null) {//close uhf module
            mUhfrManager.close();
            mUhfrManager = null;
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void closePSAM() {
        psam.closeRfid();
        sendMes("关闭PSAM卡");
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String RFID_Trace() {
        //启动天线读取
        mUhfrManager = UHFRManager.getIntance();

        mUhfrManager.setCancleFastMode();

        mUhfrManager.setPower(10, 10);
        List<Reader.TAGINFO> list1;
        list1 = mUhfrManager.tagInventoryByTimer((short) 100);
        //过滤非法及未上电标签
        String epc = "";
        String hasElecEpc = "";
        List<Reader.TAGINFO> list2;

        if (list1 != null && list1.size() > 0) {
            list2 = new ArrayList<Reader.TAGINFO>();
            for (Reader.TAGINFO tfs : list1) {
                byte[] epcdata = tfs.EpcId;
                epc = Tools.Bytes2HexString(epcdata, epcdata.length);

                TagEpcData ted = EpcReader.readEpc(epc);
                sendMes("读取到EPC" + epc);

                if (ted != null) {
                    sendMes("包号:" + ted.getTagid() + " 上电状态:" + ted.getHasElec() + " 锁状态:" + ted.getLockstuts() + " 异常标志:" + ted.getLockeEx() + " EPC异常:" + ted.getEpcEx() + " 工作状态:" + ted.getJobstuts());
                    // if(ted.getTagid()>1000000000L&&ted.getHasElec())//如果存在大于0的包号并且已经上电
                    if (ted.getTagid() > 1000000000L)//如果存在大于0的包号 不需要上电
                    {
                        list2.add(tfs);
                        hasElecEpc = epc;
                    }
                }
            }
        } else {
            closeUHF();
            return LOCK_ERR.LOCK_NOEPC_ERR + "";
        }
        //查找信号最强的标签
        Reader.TAGINFO Maxtfs = null;
        if (list2 != null && list2.size() > 0) {
            int rssi = list2.get(0).RSSI;
            int maxnum = 0;
            for (int i = 0; i < list2.size(); i++) {
                if (list2.get(i).RSSI > rssi) {
                    rssi = list2.get(i).RSSI;
                    maxnum = i;
                }
            }
            Maxtfs = list2.get(maxnum);
        } else {
            closeUHF();
            return LOCK_ERR.LOCK_NOEPC_ERR + "";
        }
        byte[] Maxepcdata = Maxtfs.EpcId;
        String Maxepc = Tools.Bytes2HexString(Maxepcdata, Maxepcdata.length);
        sendMes("获取到最佳信号标签为:" + Maxepc + " 信号:" + Maxtfs.RSSI);
        sendMes("包号" + EpcReader.readEpc(Maxepc).getTagid());
        //关闭UHF天线
        mUhfrManager.stopTagInventory();
        //关闭读取延时200ms;
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUhfrManager.close();
        mUhfrManager = null;

        //打开PSAM卡
        psam = new PsamCmdUtil();
        if (psam.openRfid() != null) {
            sendMes("打开PSAM模块成功");
        }
        Error err = new Error();
        byte[] result = null;
        //复位
        result = psam.resetCard(psamCard);
        if (result != null) {
            sendMes("复位PSAM卡：" + Tools.Bytes2HexString(result, result.length));
        } else {
            sendMes("复位PSAM卡失败");
            closePSAM();
            return LOCK_ERR.LOCK_PSAM_ERR + "";
        }
        //用户验证
        boolean flag = psam.verifyUser(psamCard,
                Tools.HexString2Bytes("5053414D49303031"),
                Tools.HexString2Bytes("4D494D49535F5053414D5F55534552"), err);
        if (flag) {
            sendMes("用户验证成功：");
        } else {
            sendMes("用户验证失败，错误码：" + err.getErrCode());
            closePSAM();
            return LOCK_ERR.LOCK_PSAM_ERR + "";
        }
        //获取RFID访问密码
        String RfidPwd = "";
        result = psam.getRFIDPassword(psamCard, Tools.HexString2Bytes(Maxepc), err);
        if (result != null) {
            sendMes("获取RFID密码成功：" + Tools.Bytes2HexString(result, result.length));
            RfidPwd = Tools.Bytes2HexString(result, result.length);
        } else {

            sendMes("获取RFID密码失败，错误码：" + err.getErrCode());
            closePSAM();
            return LOCK_ERR.LOCK_PSAM_ERR + "";
        }

        //读取user区
        mUhfrManager = UHFRManager.getIntance();
        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(10, 10);
        int mbank = 3;//user区
        int startaddr = 0;
        int datalen = 32;
        byte[] rdata = new byte[datalen];
        byte[] password = Tools.HexString2Bytes(RfidPwd);
        short timeout = 500;

        byte[] fdata = Tools.HexString2Bytes(Maxepc.substring(0, 8));
        sendMes("盘点过滤值:" + Maxepc.substring(0, 8));
        int fbank = 1;
        int fstartaddr = 2;
        boolean matching = true;
        //读取指令区判断是否可操作


        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 4; i++) {
           // sendMes("第" + (1 + i) + "次读取");
            if (i == 3) {
                datalen = 12;
                rdata = new byte[datalen];
            }
            byte[] usercmddata = mUhfrManager.getTagDataByFilter(mbank, startaddr + (i * 32) , datalen, password, timeout, fdata, fbank, fstartaddr, matching);

            if (usercmddata != null) {
                //sendMes("用户区读取成功:" + Tools.Bytes2HexString(usercmddata, usercmddata.length));
                stringBuilder.append(Tools.Bytes2HexString(usercmddata, usercmddata.length));
            } else {
                sendMes("指令区读取失败!");
                stringBuilder.append(LOCK_ERR.LOCK_READUSER_ERR+"");
                //Util.play(3, 0);
                return LOCK_ERR.LOCK_READUSER_ERR+"";

               // return LOCK_ERR.LOCK_READUSER_ERR;
            }
//            Reader.READER_ERR reader_err = mUhfrManager.getTagData(mbank, startaddr + (i * 64) / 2, datalen, rdata, password, timeout);
//            if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
//               // sendMes("用户区读取成功:" + Tools.Bytes2HexString(rdata, rdata.length));
//                stringBuilder.append(Tools.Bytes2HexString(rdata, rdata.length));
//            } else {
//                //sendMes("用户区读取失败!");
//                // stringBuilder.append(LOCK_ERR.LOCK_READUSER_ERR+"");
//                //return LOCK_ERR.LOCK_READUSER_ERR+"";
//            }
        }
        String reTraceData = stringBuilder.toString();
        sendMes("用户区数据:"+reTraceData+" 长度:"+reTraceData.length());
        closeUHF();
        return reTraceData;
    }

    public LOCK_ERR RFID_TraceCmd(int logid) {
        //启动天线读取
        mUhfrManager = UHFRManager.getIntance();

        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(10, 10);
        List<Reader.TAGINFO> list1;
        list1 = mUhfrManager.tagInventoryByTimer((short) 100);
        //过滤非法及未上电标签
        String epc = "";
        String hasElecEpc = "";
        List<Reader.TAGINFO> list2;

        if (list1 != null && list1.size() > 0) {
            list2 = new ArrayList<Reader.TAGINFO>();
            for (Reader.TAGINFO tfs : list1) {
                byte[] epcdata = tfs.EpcId;
                epc = Tools.Bytes2HexString(epcdata, epcdata.length);

                TagEpcData ted = EpcReader.readEpc(epc);
                sendMes("读取到EPC" + epc);
                sendMes("包号:" + ted.getTagid() + " 上电状态:" + ted.getHasElec() + " 锁状态:" + ted.getLockstuts() + " 异常标志:" + ted.getLockeEx() + " EPC异常:" + ted.getEpcEx() + " 工作状态:" + ted.getJobstuts());
                if (ted != null) {
                    if (ted.getTagid() > 1000000000L && ted.getHasElec())//如果存在大于0的包号并且已经上电
                    {
                        list2.add(tfs);
                        hasElecEpc = epc;
                    }
                }
            }
        } else {
            closeUHF();
            return LOCK_ERR.LOCK_NOEPC_ERR;
        }
        //查找信号最强的标签
        Reader.TAGINFO Maxtfs = null;
        if (list2 != null && list2.size() > 0) {
            int rssi = list2.get(0).RSSI;
            int maxnum = 0;
            for (int i = 0; i < list2.size(); i++) {
                if (list2.get(i).RSSI > rssi) {
                    rssi = list2.get(i).RSSI;
                    maxnum = i;
                }
            }
            Maxtfs = list2.get(maxnum);
        } else {
            closeUHF();
            return LOCK_ERR.LOCK_NOEPC_ERR;
        }
        byte[] Maxepcdata = Maxtfs.EpcId;
        String Maxepc = Tools.Bytes2HexString(Maxepcdata, Maxepcdata.length);
        sendMes("获取到最佳信号标签为:" + Maxepc + " 信号:" + Maxtfs.RSSI);
        sendMes("包号" + EpcReader.readEpc(Maxepc).getTagid());
        //关闭UHF天线
        // mUhfrManager.stopTagInventory();
        //关闭读取延时200ms;

        mUhfrManager.close();
        mUhfrManager = null;
        //获取恢复加密命令
        TagEpcData tagEpcData = EpcReader.readEpc(hasElecEpc);
        sendMes("操作数为" + tagEpcData.getOperatecount());
        if (tagEpcData.getOperatecount() < logid) {
            sendMes("操作数不足，无法追溯第" + logid + "条记录");
            return LOCK_ERR.LOCK_READUSER_ERR;
        }
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //打开PSAM卡
        psam = new PsamCmdUtil();
        if (psam.openRfid() != null) {
            sendMes("打开PSAM模块成功");
        }
        Error err = new Error();
        byte[] result = null;
        //复位
        result = psam.resetCard(psamCard);
        if (result != null) {
            sendMes("复位PSAM卡：" + Tools.Bytes2HexString(result, result.length));
        } else {
            sendMes("复位PSAM卡失败");
            closePSAM();
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //用户验证
        boolean flag = psam.verifyUser(psamCard,
                Tools.HexString2Bytes("5053414D49303031"),
                Tools.HexString2Bytes("4D494D49535F5053414D5F55534552"), err);
        if (flag) {
            sendMes("用户验证成功：");
        } else {
            sendMes("用户验证失败，错误码：" + err.getErrCode());
            closePSAM();
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //获取RFID访问密码
        String RfidPwd = "";
        result = psam.getRFIDPassword(psamCard, Tools.HexString2Bytes(Maxepc), err);
        if (result != null) {
            sendMes("获取RFID密码成功：" + Tools.Bytes2HexString(result, result.length));
            RfidPwd = Tools.Bytes2HexString(result, result.length);
        } else {
            closePSAM();
            sendMes("获取RFID密码失败，错误码：" + err.getErrCode());
            return LOCK_ERR.LOCK_PSAM_ERR;
        }


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String DateTimeStr = sdf.format(new Date());
        String cmd = "";
        byte[] id = new byte[2];
        id[0] = 0x00;
        id[1] = 0x01;

        id = intToBytes(logid, 2);


        byte[] item = new byte[1];
        item[0] = 0x01;
        byte[] logflag = new byte[1];
        logflag[0] = 0x01;
        sendMes("获取追溯指令：日志序号" + Tools.Bytes2HexString(id, id.length) + "日志项数:" + Tools.Bytes2HexString(item, item.length) + "结束标志:" + Tools.Bytes2HexString(logflag, logflag.length));
        result = psam.genTraceElsCmd(psamCard, Tools.HexString2Bytes(Maxepc), id, item, logflag, err);

        if (result != null) {
            cmd = Tools.Bytes2HexString(result, result.length);
            sendMes("获取追溯指令成功：" + Tools.Bytes2HexString(result, result.length));
        } else {
            sendMes("获取追溯指令失败，错误码：" + err.getErrCode());
            closePSAM();
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        psam.closeRfid();
        sendMes("关闭PSAM卡");
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //开始写入指令操作
        mUhfrManager = UHFRManager.getIntance();
        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(10, 10);
        int mbank = 3;//user区
        int startaddr = 76;
        int datalen = 48;
        byte rdata[] = new byte[datalen];
        byte[] password = Tools.HexString2Bytes(RfidPwd);
        short timeout = 500;

        byte[] fdata = Tools.HexString2Bytes(Maxepc.substring(0, 8));
        sendMes("盘点过滤值:" + Maxepc.substring(0, 8));
        int fbank = 1;
        int fstartaddr = 2;
        boolean matching = true;
        //读取指令区判断是否可操作
        byte[] usercmddata = mUhfrManager.getTagDataByFilter(mbank, startaddr, datalen, password, timeout, fdata, fbank, fstartaddr, matching);
        if (usercmddata != null) {
            sendMes("指令区读取成功:" + Tools.Bytes2HexString(usercmddata, usercmddata.length));
        } else {
            sendMes("指令区读取失败!");
            Util.play(3, 0);
            return LOCK_ERR.LOCK_READUSER_ERR;
        }

//        Reader.READER_ERR reader_err = mUhfrManager.getTagData(mbank, startaddr, datalen, rdata, password, timeout);
//        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
//            sendMes("指令区读取成功:" + Tools.Bytes2HexString(rdata, rdata.length));
//        } else {
//            sendMes("指令区读取失败!");
//            closeUHF();
//            return LOCK_ERR.LOCK_READUSER_ERR;
//        }

        if (!Tools.Bytes2HexString(usercmddata, usercmddata.length).startsWith("BB"))//如果不是BB开头，不可以操作写入指令
        {
            sendMes("不是BB开头，不可以操作写入指令!");
            closeUHF();
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        }
        //指令区可操作，开始写入
        byte[] cmdB = Tools.HexString2Bytes("66" + "08" + cmd);
        sendMes("写入指令区:" + "66" + "08" + cmd + " 长度:" + cmdB.length);
        mUhfrManager.setPower(10, 10);
        sendMes("设置读取功率：" + 10 + " 写入功率：" + 10);

        Reader.READER_ERR reader_err = mUhfrManager.writeTagDataByFilter((char) mbank, startaddr, cmdB, cmdB.length, password, timeout, fdata, fbank, fstartaddr, matching);
        // reader_err = mUhfrManager.writeTagData((char) mbank, startaddr, cmdB, cmdB.length, password, timeout);
        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
            sendMes("指令区写入成功:" + cmd);
            sendMes("等待3秒....");
            //写入成功后，等待4秒
            try {
                Thread.sleep(4000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendMes("开始读取指令区结果:");
            mUhfrManager.setInventoryFilter(fdata, fbank, fstartaddr, matching);
            usercmddata = mUhfrManager.getTagDataByFilter(mbank, startaddr, datalen, password, timeout, fdata, fbank, fstartaddr, matching);
            //  reader_err = mUhfrManager.getTagData(mbank, startaddr, datalen, rdata, password, timeout);
            if (usercmddata != null) {
                // sendMes("指令区读取成功:" + Tools.Bytes2HexString(usercmddata, usercmddata.length));
                String alltracedata = Tools.Bytes2HexString(usercmddata, usercmddata.length);
                sendMes("指令区读取成功:追溯数据为:" + alltracedata);
                if (alltracedata.substring(4, 68).startsWith(cmd))//如果追溯数据与指令相同
                {
                    sendMes("指令区未变化,追溯失败!");
                    closeUHF();
                    return LOCK_ERR.LOCK_READUSER_ERR;
                }
                closeUHF();
                PsamCmdUtil psam = new PsamCmdUtil();
                if (psam.openRfid() != null) {
                    sendMes("打开PSAM模块成功");
                }
                //复位
                result = psam.resetCard(1);
                if (result != null) {
                    sendMes("复位PSAM卡：" + Tools.Bytes2HexString(result, result.length));
                } else {
                    closePSAM();
                    sendMes("复位PSAM卡失败");
                    return LOCK_ERR.LOCK_PSAM_ERR;
                    // Util.play(3, 0);
                }
                //用户验证
                flag = psam.verifyUser(1,
                        Tools.HexString2Bytes("5053414D49303031"),
                        Tools.HexString2Bytes("4D494D49535F5053414D5F55534552"), err);
                if (flag) {
                    sendMes("用户验证成功：");
                } else {
                    sendMes("用户验证失败，错误码：");
                    closePSAM();
                    //Util.play(3, 0);
                    return LOCK_ERR.LOCK_PSAM_ERR;
                }
                sendMes("开始解密追溯数据:" + alltracedata.substring(4, 68));
                byte[] rs = psam.decryptElsData(1, Tools.HexString2Bytes(hasElecEpc), Tools.HexString2Bytes(alltracedata.substring(4, 68)), err);
                if (rs != null) {
                    sendMes("追溯数据为:" + Tools.Bytes2HexString(rs, rs.length));
                    closePSAM();
                    return LOCK_ERR.LOCK_OK;
                } else {
                    closePSAM();
                    sendMes("追溯数据解密失败!");
                    return LOCK_ERR.LOCK_READUSER_ERR;
                }
            } else {
                sendMes("指令区读取失败!");
                closePSAM();
                return LOCK_ERR.LOCK_READUSER_ERR;
            }
        } else {
            sendMes("指令区读取失败!");
            closeUHF();
            //  Util.play(3, 0);
            return LOCK_ERR.LOCK_READUSER_ERR;
        }
//            if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
//                String alltracedata=Tools.Bytes2HexString(rdata, rdata.length);
//                sendMes("指令区读取成功:追溯数据为:" +alltracedata);
//               if(alltracedata.substring(4,68).startsWith(cmd))//如果追溯数据与指令相同
//                {
//                    sendMes("指令区未变化,追溯失败!");
//                    closeUHF();
//                    return LOCK_ERR.LOCK_READUSER_ERR;
//                }
//                closeUHF();

    }

    public LOCK_ERR RFID_Recovery(String operator1str, String operator2str) {
        //启动天线读取
        mUhfrManager = UHFRManager.getIntance();

        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(10, 10);
        List<Reader.TAGINFO> list1;
        list1 = mUhfrManager.tagInventoryByTimer((short) 100);
        //过滤非法及未上电标签
        String epc = "";
        String hasElecEpc = "";
        List<Reader.TAGINFO> list2;

        if (list1 != null && list1.size() > 0) {
            list2 = new ArrayList<Reader.TAGINFO>();
            for (Reader.TAGINFO tfs : list1) {
                byte[] epcdata = tfs.EpcId;
                epc = Tools.Bytes2HexString(epcdata, epcdata.length);

                TagEpcData ted = EpcReader.readEpc(epc);
                sendMes("读取到EPC" + epc);
                sendMes("包号:" + ted.getTagid() + " 上电状态:" + ted.getHasElec() + " 锁状态:" + ted.getLockstuts() + " 异常标志:" + ted.getLockeEx() + " EPC异常:" + ted.getEpcEx() + " 工作状态:" + ted.getJobstuts());
                if (ted != null) {
                    if (ted.getTagid() > 1000000000L && ted.getHasElec())//如果存在大于0的包号并且已经上电
                    {
                        list2.add(tfs);
                        hasElecEpc = epc;
                    }
                }
            }
        } else {
            closeUHF();
            return LOCK_ERR.LOCK_NOEPC_ERR;
        }
        //查找信号最强的标签
        Reader.TAGINFO Maxtfs = null;
        if (list2 != null && list2.size() > 0) {
            int rssi = list2.get(0).RSSI;
            int maxnum = 0;
            for (int i = 0; i < list2.size(); i++) {
                if (list2.get(i).RSSI > rssi) {
                    rssi = list2.get(i).RSSI;
                    maxnum = i;
                }
            }
            Maxtfs = list2.get(maxnum);
        } else {
            closeUHF();
            return LOCK_ERR.LOCK_NOEPC_ERR;
        }
        byte[] Maxepcdata = Maxtfs.EpcId;
        String Maxepc = Tools.Bytes2HexString(Maxepcdata, Maxepcdata.length);
        sendMes("获取到最佳信号标签为:" + Maxepc + " 信号:" + Maxtfs.RSSI);
        sendMes("包号" + EpcReader.readEpc(Maxepc).getTagid());
        //关闭UHF天线
        mUhfrManager.stopTagInventory();
        //关闭读取延时200ms;
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUhfrManager.close();
        mUhfrManager = null;

        //打开PSAM卡
        psam = new PsamCmdUtil();
        if (psam.openRfid() != null) {
            sendMes("打开PSAM模块成功");
        }
        Error err = new Error();
        byte[] result = null;
        //复位
        result = psam.resetCard(psamCard);
        if (result != null) {
            sendMes("复位PSAM卡：" + Tools.Bytes2HexString(result, result.length));
        } else {
            sendMes("复位PSAM卡失败");
            closePSAM();
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //用户验证
        boolean flag = psam.verifyUser(psamCard,
                Tools.HexString2Bytes("5053414D49303031"),
                Tools.HexString2Bytes("4D494D49535F5053414D5F55534552"), err);
        if (flag) {
            sendMes("用户验证成功：");
        } else {
            sendMes("用户验证失败，错误码：" + err.getErrCode());
            closePSAM();
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //获取RFID访问密码
        String RfidPwd = "";
        result = psam.getRFIDPassword(psamCard, Tools.HexString2Bytes(Maxepc), err);
        if (result != null) {
            sendMes("获取RFID密码成功：" + Tools.Bytes2HexString(result, result.length));
            RfidPwd = Tools.Bytes2HexString(result, result.length);
        } else {
            closePSAM();
            sendMes("获取RFID密码失败，错误码：" + err.getErrCode());
            return LOCK_ERR.LOCK_PSAM_ERR;
        }

        //获取恢复加密命令
        String cmd = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String DateTimeStr = sdf.format(new Date());

        result = psam.genRecoverElsCmd(psamCard, Tools.HexString2Bytes(Maxepc), 0, Tools.HexString2Bytes(operator1str), Tools.HexString2Bytes(operator2str), DateTimeStr, err);


        if (result != null) {
            cmd = Tools.Bytes2HexString(result, result.length);
            sendMes("获取恢复指令成功：" + Tools.Bytes2HexString(result, result.length));
        } else {
            sendMes("获取恢复指令失败，错误码：" + err.getErrCode());
            closePSAM();
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        psam.closeRfid();
        sendMes("关闭PSAM卡");

        //开始写入指令操作
        mUhfrManager = UHFRManager.getIntance();
        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(10, 10);
        int mbank = 3;//user区
        int startaddr = 76;
        int datalen = "6618B05CF3E99DEC4EE0747D83890C40FA01075AE5976A32DD51".length() / 2;
        byte rdata[] = new byte[datalen];
        byte[] password = Tools.HexString2Bytes(RfidPwd);
        short timeout = 500;
        //读取指令区判断是否可操作
        Reader.READER_ERR reader_err = mUhfrManager.getTagData(mbank, startaddr, datalen, rdata, password, timeout);
        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
            sendMes("指令区读取成功:" + Tools.Bytes2HexString(rdata, rdata.length));
        } else {
            sendMes("指令区读取失败!");
            closeUHF();
            return LOCK_ERR.LOCK_READUSER_ERR;
        }

        if (!Tools.Bytes2HexString(rdata, rdata.length).startsWith("BB"))//如果不是BB开头，不可以操作写入指令
        {
            sendMes("不是BB开头，不可以操作写入指令!");
            closeUHF();
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        }
        //指令区可操作，开始写入
        byte[] cmdB = Tools.HexString2Bytes("66" + intToHex(cmd.length() / 2) + cmd);
        sendMes("写入指令区:" + "66" + intToHex(cmd.length() / 2) + cmd + " 长度:" + cmdB.length);
        mUhfrManager.setPower(10, 10);
        sendMes("设置读取功率：" + 10 + " 写入功率：" + 10);
        reader_err = mUhfrManager.writeTagData((char) mbank, startaddr, cmdB, cmdB.length, password, timeout);
        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
            sendMes("指令区写入成功:");
            closeUHF();
            return LOCK_ERR.LOCK_OK;
        } else {
            sendMes("指令区写入失败!");
            closeUHF();
            return LOCK_ERR.LOCK_WRITEUSER_ERR;
        }
    }

    public LOCK_ERR RFID_Reset() {
        //启动天线读取
        mUhfrManager = UHFRManager.getIntance();

        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(10, 10);
        List<Reader.TAGINFO> list1;
        list1 = mUhfrManager.tagInventoryByTimer((short) 100);
        //过滤非法及未上电标签
        String epc = "";
        String hasElecEpc = "";
        List<Reader.TAGINFO> list2;

        if (list1 != null && list1.size() > 0) {
            list2 = new ArrayList<Reader.TAGINFO>();
            for (Reader.TAGINFO tfs : list1) {
                byte[] epcdata = tfs.EpcId;
                epc = Tools.Bytes2HexString(epcdata, epcdata.length);

                TagEpcData ted = EpcReader.readEpc(epc);
                sendMes("读取到EPC" + epc);
                sendMes("包号:" + ted.getTagid() + " 上电状态:" + ted.getHasElec() + " 锁状态:" + ted.getLockstuts() + " 异常标志:" + ted.getLockeEx() + " EPC异常:" + ted.getEpcEx() + " 工作状态:" + ted.getJobstuts());
                if (ted != null) {
                    if (ted.getTagid() > 1000000000L && ted.getHasElec())//如果存在大于0的包号并且已经上电
                    {
                        list2.add(tfs);
                        hasElecEpc = epc;
                    }
                }
            }
        } else {
            closeUHF();
            return LOCK_ERR.LOCK_NOEPC_ERR;
        }
        //查找信号最强的标签
        Reader.TAGINFO Maxtfs = null;
        if (list2 != null && list2.size() > 0) {
            int rssi = list2.get(0).RSSI;
            int maxnum = 0;
            for (int i = 0; i < list2.size(); i++) {
                if (list2.get(i).RSSI > rssi) {
                    rssi = list2.get(i).RSSI;
                    maxnum = i;
                }
            }
            Maxtfs = list2.get(maxnum);
        } else {
            closeUHF();
            return LOCK_ERR.LOCK_NOEPC_ERR;
        }
        byte[] Maxepcdata = Maxtfs.EpcId;
        String Maxepc = Tools.Bytes2HexString(Maxepcdata, Maxepcdata.length);
        sendMes("获取到最佳信号标签为:" + Maxepc + " 信号:" + Maxtfs.RSSI);
        sendMes("包号" + EpcReader.readEpc(Maxepc).getTagid());
        //关闭UHF天线
        mUhfrManager.stopTagInventory();
        //关闭读取延时200ms;
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUhfrManager.close();
        mUhfrManager = null;

        //打开PSAM卡
        psam = new PsamCmdUtil();
        if (psam.openRfid() != null) {
            sendMes("打开PSAM模块成功");
        }
        Error err = new Error();
        byte[] result = null;
//        //复位
        result = psam.resetCard(psamCard);
        if (result != null) {
            sendMes("复位PSAM卡：" + Tools.Bytes2HexString(result, result.length));
        } else {
            sendMes("复位PSAM卡失败");
            closePSAM();
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //用户验证
        boolean flag = psam.verifyUser(psamCard,
                Tools.HexString2Bytes("5053414D49303031"),
                Tools.HexString2Bytes("4D494D49535F5053414D5F55534552"), err);
        if (flag) {
            sendMes("用户验证成功：");
        } else {
            sendMes("用户验证失败，错误码：" + err.getErrCode());
            closePSAM();
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //获取RFID访问密码
        String RfidPwd = "";
        result = psam.getRFIDPassword(psamCard, Tools.HexString2Bytes(Maxepc), err);
        if (result != null) {
            sendMes("获取RFID密码成功：" + Tools.Bytes2HexString(result, result.length));
            RfidPwd = Tools.Bytes2HexString(result, result.length);
        } else {
            closePSAM();
            sendMes("获取RFID密码失败，错误码：" + err.getErrCode());
            return LOCK_ERR.LOCK_PSAM_ERR;
        }

        //获取删除加密命令
        String cmd = "8800";
        sendMes("获取指令成功：" + cmd);

        psam.closeRfid();
        sendMes("关闭PSAM卡");

        //开始写入指令操作
        mUhfrManager = UHFRManager.getIntance();
        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(10, 10);
        int mbank = 3;//user区
        int startaddr = 76;
        int datalen = cmd.length() / 2;
        byte rdata[] = new byte[datalen];
        byte[] password = Tools.HexString2Bytes(RfidPwd);
        short timeout = 500;

        //设置只盘点当前标签
        byte[] fdata = Tools.HexString2Bytes(Maxepc.substring(0, 8));
        int fbank = 1;
        int fstartaddr = 2;
        boolean matching = true;
        //读取指令区判断是否可操作
        byte[] usercmddata = mUhfrManager.getTagDataByFilter(mbank, startaddr, datalen, password, timeout, fdata, fbank, fstartaddr, matching);
        if (usercmddata != null) {
            sendMes("指令区读取成功:" + Tools.Bytes2HexString(usercmddata, usercmddata.length));
        } else {
            sendMes("指令区读取失败!");
            closeUHF();
            Util.play(3, 0);
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        }

        if (!Tools.Bytes2HexString(usercmddata, usercmddata.length).startsWith("BB"))//如果不是BB开头，不可以操作写入指令
        {
            sendMes("不是BB开头，不可以操作写入指令!");
            closeUHF();
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        }
        //指令区可操作，开始写入
        byte[] cmdB = Tools.HexString2Bytes(cmd);
        sendMes("写入指令区:" + cmd + " 长度:" + cmdB.length);
        mUhfrManager.setPower(10, 10);
        sendMes("设置读取功率：" + 10 + " 写入功率：" + 10);
        Reader.READER_ERR reader_err = mUhfrManager.writeTagDataByFilter((char) mbank, startaddr, cmdB, cmdB.length, password, timeout, fdata, fbank, fstartaddr, matching);
        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
            sendMes("指令区写入成功:" + cmd);
//            closeUHF();
//            return LOCK_ERR.LOCK_OK;
        } else {
            sendMes("指令区写入失败!");
            closeUHF();
            return LOCK_ERR.LOCK_WRITEUSER_ERR;
        }

        //写入成功，开始判断操作结果
        sendMes("等待2秒....");
        //写入成功后，等待2秒
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mUhfrManager.setInventoryFilter(fdata, fbank, fstartaddr, matching);
        sendMes("设置盘点过滤值：" + Maxepc.substring(0, 8));
        sendMes("盘点过滤包号：" + EpcReader.readEpc(Maxepc).getTagid());
        //盘点5秒钟
//
        List<Reader.TAGINFO> listrs;
        listrs = mUhfrManager.tagInventoryByTimer((short) 100);
        if (listrs != null && listrs.size() > 0) {
            sendMes("盘点到结果标签" + listrs.size() + "次");
            for (Reader.TAGINFO tfs : listrs) {
                byte[] epcdata = tfs.EpcId;
                String epcrs = Tools.Bytes2HexString(epcdata, epcdata.length);
                TagEpcData ted = EpcReader.readEpc(epcrs);
                sendMes("盘存到操作后标签：" + epc);
                sendMes("包号:" + ted.getTagid());
                sendMes("重置后券别:" + ted.getPervalueid());
                sendMes("重置后上电状态:" + ted.getHasElec());
                // if(ted.getHasElec()==false&&ted.getPervalueid()==0)//重置正常
                if (ted.getHasElec() == true && ted.getPervalueid() == 0)//重置正常
                {
                    sendMes("锁状态正常！");
                    closeUHF();
                    return LOCK_ERR.LOCK_OK;
                }
            }
            sendMes("未盘点正确结果标签，重置失败!");
            closeUHF();
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        } else {
            sendMes("未盘点到结果标签，重置失败!");
            closeUHF();
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        }
    }

    public LOCK_ERR RFID_Delete(String operator1str, String operator2str, boolean ischecknum) {
        //启动天线读取
        mUhfrManager = UHFRManager.getIntance();

        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(10, 10);
        List<Reader.TAGINFO> list1;
        list1 = mUhfrManager.tagInventoryByTimer((short) 100);
        //过滤非法及未上电标签
        String epc = "";
        String hasElecEpc = "";
        List<Reader.TAGINFO> list2;

        if (list1 != null && list1.size() > 0) {
            list2 = new ArrayList<Reader.TAGINFO>();
            for (Reader.TAGINFO tfs : list1) {
                byte[] epcdata = tfs.EpcId;
                epc = Tools.Bytes2HexString(epcdata, epcdata.length);

                TagEpcData ted = EpcReader.readEpc(epc);
                sendMes("读取到EPC" + epc);
                sendMes("包号:" + ted.getTagid() + " 上电状态:" + ted.getHasElec() + " 锁状态:" + ted.getLockstuts() + " 异常标志:" + ted.getLockeEx() + " EPC异常:" + ted.getEpcEx() + " 工作状态:" + ted.getJobstuts());
                if (ted != null) {
                    if (ted.getTagid() > 1000000000L && ted.getHasElec())//如果存在大于0的包号并且已经上电
                    {
                        list2.add(tfs);
                        hasElecEpc = epc;
                    }
                }
            }
        } else {
            closeUHF();
            return LOCK_ERR.LOCK_NOEPC_ERR;
        }
        //查找信号最强的标签
        Reader.TAGINFO Maxtfs = null;
        if (list2 != null && list2.size() > 0) {
            int rssi = list2.get(0).RSSI;
            int maxnum = 0;
            for (int i = 0; i < list2.size(); i++) {
                if (list2.get(i).RSSI > rssi) {
                    rssi = list2.get(i).RSSI;
                    maxnum = i;
                }
            }
            Maxtfs = list2.get(maxnum);
        } else {
            closeUHF();
            return LOCK_ERR.LOCK_NOEPC_ERR;
        }
        byte[] Maxepcdata = Maxtfs.EpcId;
        String Maxepc = Tools.Bytes2HexString(Maxepcdata, Maxepcdata.length);
        sendMes("获取到最佳信号标签为:" + Maxepc + " 信号:" + Maxtfs.RSSI);
        sendMes("包号" + EpcReader.readEpc(Maxepc).getTagid());


        //关闭UHF天线
        mUhfrManager.stopTagInventory();
        //关闭读取延时200ms;
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUhfrManager.close();
        mUhfrManager = null;
        int operatecount = EpcReader.readEpc(Maxepc).getOperatecount();
        sendMes("操作数为:" + operatecount);

        if (ischecknum)//如果检查操作数的话
        {
            if (operatecount < 6) {
                sendMes("操作数小于6不可执行清除指令");
                return LOCK_ERR.LOCK_NOEPC_ERR;
            }

        }


        //打开PSAM卡
        psam = new PsamCmdUtil();
        if (psam.openRfid() != null) {
            sendMes("打开PSAM模块成功");
        }
        Error err = new Error();
        byte[] result = null;
//        //复位
        result = psam.resetCard(psamCard);
        if (result != null) {
            sendMes("复位PSAM卡：" + Tools.Bytes2HexString(result, result.length));
        } else {
            sendMes("复位PSAM卡失败");
            closePSAM();
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //用户验证
        boolean flag = psam.verifyUser(psamCard,
                Tools.HexString2Bytes("5053414D49303031"),
                Tools.HexString2Bytes("4D494D49535F5053414D5F55534552"), err);
        if (flag) {
            sendMes("用户验证成功：");
        } else {
            closePSAM();
            sendMes("用户验证失败，错误码：" + err.getErrCode());
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        //获取RFID访问密码
        String RfidPwd = "";
        result = psam.getRFIDPassword(psamCard, Tools.HexString2Bytes(Maxepc), err);
        if (result != null) {
            sendMes("获取RFID密码成功：" + Tools.Bytes2HexString(result, result.length));
            RfidPwd = Tools.Bytes2HexString(result, result.length);
        } else {
            closePSAM();
            sendMes("获取RFID密码失败，错误码：" + err.getErrCode());
            return LOCK_ERR.LOCK_PSAM_ERR;
        }

        //获取删除加密命令
        String cmd = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String DateTimeStr = sdf.format(new Date());

        result = psam.genClenLogElsCmd(psamCard, Tools.HexString2Bytes(Maxepc), Tools.HexString2Bytes(operator1str), Tools.HexString2Bytes(operator2str), DateTimeStr, err);


        if (result != null) {
            cmd = Tools.Bytes2HexString(result, result.length);
            sendMes("获取删除指令成功：" + Tools.Bytes2HexString(result, result.length));
        } else {
            closePSAM();
            sendMes("获取删除指令失败，错误码：" + err.getErrCode());
            return LOCK_ERR.LOCK_PSAM_ERR;
        }
        psam.closeRfid();
        sendMes("关闭PSAM卡");

        //开始写入指令操作
        mUhfrManager = UHFRManager.getIntance();
        mUhfrManager.setCancleFastMode();
        mUhfrManager.setPower(10, 10);
        int mbank = 3;//user区
        int startaddr = 76;
        int datalen = "6618B05CF3E99DEC4EE0747D83890C40FA01075AE5976A32DD51".length() / 2;
        byte rdata[] = new byte[datalen];
        byte[] password = Tools.HexString2Bytes(RfidPwd);
        short timeout = 500;
        //读取指令区判断是否可操作
        Reader.READER_ERR reader_err = mUhfrManager.getTagData(mbank, startaddr, datalen, rdata, password, timeout);
        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
            sendMes("指令区读取成功:" + Tools.Bytes2HexString(rdata, rdata.length));
        } else {
            sendMes("指令区读取失败!");
            closeUHF();
            return LOCK_ERR.LOCK_READUSER_ERR;
        }

        if (!Tools.Bytes2HexString(rdata, rdata.length).startsWith("BB"))//如果不是BB开头，不可以操作写入指令
        {
            sendMes("不是BB开头，不可以操作写入指令!");
            closeUHF();
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        }
        //指令区可操作，开始写入
        byte[] cmdB = Tools.HexString2Bytes("66" + intToHex(cmd.length() / 2) + cmd);
        sendMes("写入指令区:" + "66" + intToHex(cmd.length() / 2) + cmd + " 长度:" + cmdB.length);
        mUhfrManager.setPower(10, 10);
        sendMes("设置读取功率：" + 10 + " 写入功率：" + 10);
        reader_err = mUhfrManager.writeTagData((char) mbank, startaddr, cmdB, cmdB.length, password, timeout);
        if (reader_err == Reader.READER_ERR.MT_OK_ERR) {
            sendMes("指令区写入成功");
            //closeUHF();
            //return LOCK_ERR.LOCK_OK;
        } else {
            sendMes("指令区写入失败!");
            closeUHF();
            return LOCK_ERR.LOCK_WRITEUSER_ERR;
        }

        //写入成功，开始判断操作结果
        sendMes("等待8秒....");
        //写入成功后，等待2秒
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //设置只盘点当前标签
        byte[] fdata = Tools.HexString2Bytes(Maxepc.substring(0, 8));
        int fbank = 1;
        int fstartaddr = 2;
        boolean matching = true;
        mUhfrManager.setInventoryFilter(fdata, fbank, fstartaddr, matching);
        sendMes("设置盘点过滤值：" + Maxepc.substring(0, 8));
        sendMes("盘点过滤包号：" + EpcReader.readEpc(Maxepc).getTagid());
        //盘点5秒钟
//
        List<Reader.TAGINFO> listrs;
        listrs = mUhfrManager.tagInventoryByTimer((short) 100);
        if (listrs != null && listrs.size() > 0) {
            sendMes("盘点到结果标签" + listrs.size() + "次");
            for (Reader.TAGINFO tfs : listrs) {
                byte[] epcdata = tfs.EpcId;
                String epcrs = Tools.Bytes2HexString(epcdata, epcdata.length);
                TagEpcData ted = EpcReader.readEpc(epcrs);
                sendMes("盘存到清除操作后标签：" + epc);
                sendMes("包号:" + ted.getTagid());
                sendMes("清除后操作数:" + ted.getOperatecount());


                if (ted.getOperatecount() == 1)//重置正常
                {
                    sendMes("锁状态正常！");
                    closeUHF();
                    return LOCK_ERR.LOCK_OK;
                }
            }
            sendMes("未盘点正确结果标签，清除失败!");
            closeUHF();
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        } else {
            sendMes("未盘点到结果标签，清除失败!");
            closeUHF();
            return LOCK_ERR.LOCK_LOCKRRS_ERR;
        }
    }

    private static String intToHex(int n) {
        StringBuffer s = new StringBuffer();
        String a;
        char[] b = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        while (n != 0) {
            s = s.append(b[n % 16]);
            n = n / 16;
        }
        a = s.reverse().toString();
        return a;
    }

    private void sendMes(String mes) {
        Log.e(logtag, mes);
        Message message = new Message();
        message.what = INFO_MES;
        Bundle data = new Bundle();
        data.putString("message", mes);
        message.setData(data);
        mHandler.sendMessage(message);
    }

    public static byte[] intToBytes(int a, int length) {
        byte[] bs = new byte[length];
        for (int i = bs.length - 1; i >= 0; i--) {
            bs[i] = (byte) (a % 255);
            a = a / 255;
        }
        return bs;
    }

    public static enum LOCK_ERR {
        LOCK_OK(0),
        LOCK_NOEPC_ERR(1),
        LOCK_PSAM_ERR(2),
        LOCK_READUSER_ERR(3),
        LOCK_WRITEUSER_ERR(4),
        LOCK_LOCKRRS_ERR(5);

        private int value = 0;

        private LOCK_ERR(int value) {
            this.value = value;
        }

        public static LockHelper.LOCK_ERR valueOf(int value) {
            switch (value) {
                case 0:
                    return LOCK_OK;
                case 1:
                    return LOCK_NOEPC_ERR;
                case 2:
                    return LOCK_PSAM_ERR;
                case 3:
                    return LOCK_READUSER_ERR;
                case 4:
                    return LOCK_WRITEUSER_ERR;
                case 5:
                    return LOCK_LOCKRRS_ERR;
                default:
                    return LOCK_LOCKRRS_ERR;
            }

        }

        public int value() {
            return this.value;
        }
    }
}
