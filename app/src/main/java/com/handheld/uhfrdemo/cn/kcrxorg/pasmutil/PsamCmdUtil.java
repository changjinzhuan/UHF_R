package com.handheld.uhfrdemo.cn.kcrxorg.pasmutil;

import android.util.Log;

import com.BRMicro.Tools;
import com.fxpsam.nativeJni.RfidNative;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

//PSAM指令的封装
public class PsamCmdUtil {

    public static String TAG = "PsamCmdUtil" ;

    private RfidNative rfid ;

    private int port = 14 ;
    private int baudrate = 115200 ;

    private byte[] psamID = new byte[4];//00030DC8

    //响应OK
    private static String RES_OK = "9000";
    public PsamCmdUtil() {

    }

    //打开 PSAM模块
    public RfidNative openRfid(){
try
{
    if (rfid == null) {
        rfid = new RfidNative() ;
        rfid.open(port, baudrate);
    }
    return rfid ;
}catch (Exception e)
{
    Log.e("test",e.getMessage());
    return null;
}

    }

    //关闭PSAM模块
    public void closeRfid() {
        if (rfid != null) {
            rfid.close(port);
            rfid = null ;
        }
    }

    //激活卡片
    public byte[] resetCard(int psam){
        byte[] res = null ;
        byte[] result = new byte[256] ;
        int len = rfid.psamreset(psam, result) ;
        if (len > 0) {
            res = new byte[len] ;
            MyLogger.e(TAG, "resetCard :" + Tools.Bytes2HexString(result, len));
            System.arraycopy(result, 0, res, 0, len);
        }
        return res ;
    }

    //获取信息 GET_INFO  8010010015
    public byte[] getPSAMinfo(int psam , Error err) {
        byte[] result = new byte[256] ;
        byte[] info = null ;
        byte[] cmd = Tools.HexString2Bytes("8010010015") ;
        int len = rfid.psamapdu(psam, cmd, result);
        if (len > 0) {
            String resultStr = Tools.Bytes2HexString(result, len) ;
            MyLogger.e(TAG, "getPSAMinfo :" + resultStr);

            if(resultStr.endsWith(RES_OK)){
                //命令执行成功
                info = new byte[8] ;
                //psamID
                System.arraycopy(result, 0, psamID, 0, 4);
                //用户ID
                System.arraycopy(result, 5,info, 0, 8);
                MyLogger.e(TAG, "getPSAMinfo info:" + Tools.Bytes2HexString(info, info.length));
            }else{
                err.setErrCode(resultStr);
            }

            //测试返回：00030DC8 11 5053414D49303031 20190726 20200725 9000
        }
        return info ;
    }

    //获取PSAM数据证书  8010020000//证书的实际长度为226字节，
    public byte[] getPSAMCertifi(int psam ,Error err) {
        byte[] result = new byte[512] ;
        byte[] certifi = new byte[226] ;
        byte[] cmd = Tools.HexString2Bytes("8010020080") ;
        int len = rfid.psamapdu(psam, cmd, result);
        if (len > 0) {
            String resultStr = Tools.Bytes2HexString(result, len) ;
            MyLogger.e(TAG, "getPSAMCertifi :" + resultStr);

            if(resultStr.endsWith("6180")){
                //6Cxx 出错 Le长度错误，实际长度是xx  6C62
                //61xx代表后续还xx个数据未接收的
                //先将第一部分数据取出  128(0x80)字节
                System.arraycopy(result, 0, certifi, 0, 128);
                //读第二部分数据 98()0x62字节
                cmd = Tools.HexString2Bytes("8010020162") ;
                len = rfid.psamapdu(psam, cmd, result);
                if(len > 0){
                    System.arraycopy(result, 0, certifi, 128, 98);
                    MyLogger.e(TAG, "getPSAMCertifi finish:" + Tools.Bytes2HexString(result, len));
                }
            }else{
                err.setErrCode(resultStr);
            }
        }
        return certifi ;
    }

    //用户验证(VERIFY_USER)   80200000+数据域长度+用户验证信息(用户ID+用户PIN码)
    public boolean verifyUser(int psam, byte[] userID, byte[] userPIN, Error err){
        byte[] result = new byte[256] ;
        boolean flag = false ;
        String userIDStr = Tools.Bytes2HexString(userID, userID.length) ;
        String userPINStr = Tools.Bytes2HexString(userPIN, userPIN.length) ;
        byte[] length = new byte[1];
        length[0] =(byte) (userID.length + userPIN.length) ;
        //  80200000+数据域长度+用户验证信息(用户ID+用户PIN码)
        String cmdStr = "80200000" + Tools.Bytes2HexString(length, 1) + userIDStr + userPINStr ;
        byte[] cmd = Tools.HexString2Bytes(cmdStr) ;
        int len = rfid.psamapdu(psam, cmd, result);
        if (len > 0) {
            String resultStr = Tools.Bytes2HexString(result, len) ;
            MyLogger.e(TAG, "verifyUser :" + resultStr);
            if(resultStr.endsWith(RES_OK)){
                //命令执行成功9000
                flag = true ;
            }else{
                err.setErrCode(resultStr);
            }
        }
        return flag ;

    }

    //签封激活指令0x01
    //数据(交互指令DATA):面额（1字节）+ 随机数(4字节) + 传输保护密钥(8字节) + 保留(1字节) bit)
    public byte[] genTagActiveElsCmd(int psam, byte[] epc, byte volume, Error err) {
        byte[] elsCmd = null;
        String dataStr ;
        byte[] data ;
        String volumeStr ;
        byte[] cmdType ;
        //劵别：
        volumeStr = Tools.Bytes2HexString(new byte[]{volume}, 1);
        dataStr = volumeStr
                + "00000000"//随机数
                + "0000000000000000"//传输保护密钥
                + "00";
        data = Tools.HexString2Bytes(dataStr) ;
        //开锁指令0x01
        cmdType = new byte[]{0x01};
        //
        elsCmd = genElsCmd(psam, cmdType, epc, data, err) ;

        return elsCmd ;
    }

    //开锁指令0x03
    //数据(交互指令DATA):张数（2字节）+ 操作员1(4字节) + 操作员2(4字节) + 操作时间(4字节) + 手持机ID(PSAM卡ID后20bit) + 位置序号(4bit) + 随机数(4字节)+保留(1字节)
    public byte[] genOpenElsCmd(int psam, byte[] epc, int number, byte[] operator1, byte[] operator2, String dateTime, Error err) {
        byte[] elsCmd = null;
        String dataStr ;
        byte[] data ;
        byte[] dateTimeByte ;
        byte[] numberBytes = new byte[2] ;
        String psamIDStr ;
        byte[] deviceID ;
        byte[] cmdType ;

        numberBytes[0] = (byte) (number/256);
        numberBytes[1] = (byte) (number%256);
        dateTimeByte = dateTimeToByte(dateTime) ;
        psamIDStr = Tools.Bytes2HexString(psamID,psamID.length).substring(3);
        deviceID = Tools.HexString2Bytes(psamIDStr + "0") ;
        dataStr = Tools.Bytes2HexString(numberBytes, 2)
                + Tools.Bytes2HexString(operator1, operator1.length)
                + Tools.Bytes2HexString(operator2, operator2.length)
                + Tools.Bytes2HexString(dateTimeByte, dateTimeByte.length)
                + Tools.Bytes2HexString(deviceID, deviceID.length)
                + "00000000"
                + "00";
        data = Tools.HexString2Bytes(dataStr) ;
        //开锁指令0x03
        cmdType = new byte[]{0x03};
        //
        elsCmd = genElsCmd(psam, cmdType, epc, data, err) ;

        return elsCmd ;
    }

    //关锁指令0x02
    //张数（2字节）+ 操作员1(4字节) + 操作员2(4字节) + 操作时间(4字节) + 手持机ID(PSAM卡ID后20bit) + 位置序号(4bit) + 随机数(4字节)+保留(1字节)
    public byte[] genCloseElsCmd(int psam, byte[] epc, int number, byte[] operator1, byte[] operator2, String dateTime, Error err) {
        byte[] elsCmd = null;
        String dataStr ;
        byte[] data ;
        byte[] dateTimeByte ;
        byte[] numberBytes = new byte[2] ;
        String psamIDStr ;
        byte[] deviceID ;
        byte[] cmdType ;

        numberBytes[0] = (byte) (number/256);
        numberBytes[1] = (byte) (number%256);
        dateTimeByte = dateTimeToByte(dateTime) ;
        psamIDStr = Tools.Bytes2HexString(psamID,psamID.length).substring(3);
        deviceID = Tools.HexString2Bytes(psamIDStr + "0") ;
        dataStr = Tools.Bytes2HexString(numberBytes, 2)
                + Tools.Bytes2HexString(operator1, operator1.length)
                + Tools.Bytes2HexString(operator2, operator2.length)
                + Tools.Bytes2HexString(dateTimeByte, dateTimeByte.length)
                + Tools.Bytes2HexString(deviceID, deviceID.length)
                + "00000000"
                + "00";
        data = Tools.HexString2Bytes(dataStr) ;
        cmdType = new byte[]{0x02};
        //
        elsCmd = genElsCmd(psam, cmdType, epc, data, err) ;

        return elsCmd ;
    }


    //开锁写物流指令0x05
    //数据(交互指令DATA):张数（2字节）+ 操作员1(4字节) + 操作员2(4字节) + 操作时间(4字节) + 手持机ID(PSAM卡ID后20bit) + 位置序号(4bit) + 随机数(4字节)+保留(1字节)
    public byte[] genOpenWriteElsCmd(int psam, byte[] epc, int number, byte[] operator1, byte[] operator2, String dateTime, Error err) {
        byte[] elsCmd = null;
        String dataStr ;
        byte[] data ;
        byte[] dateTimeByte ;
        byte[] numberBytes = new byte[2] ;
        String psamIDStr ;
        byte[] deviceID ;
        byte[] cmdType ;

        numberBytes[0] = (byte) (number/256);
        numberBytes[1] = (byte) (number%256);
        dateTimeByte = dateTimeToByte(dateTime) ;
        psamIDStr = Tools.Bytes2HexString(psamID,psamID.length).substring(3);
        deviceID = Tools.HexString2Bytes(psamIDStr + "0") ;
        dataStr = Tools.Bytes2HexString(numberBytes, 2)
                + Tools.Bytes2HexString(operator1, operator1.length)
                + Tools.Bytes2HexString(operator2, operator2.length)
                + Tools.Bytes2HexString(dateTimeByte, dateTimeByte.length)
                + Tools.Bytes2HexString(deviceID, deviceID.length)
                + "00000000"
                + "00";
        data = Tools.HexString2Bytes(dataStr) ;
        //开锁写物流指令0x03
        cmdType = new byte[]{0x05};
        //
        elsCmd = genElsCmd(psam, cmdType, epc, data, err) ;

        return elsCmd ;
    }

    //关锁写物流指令0x04
    //张数（2字节）+ 操作员1(4字节) + 操作员2(4字节) + 操作时间(4字节) + 手持机ID(PSAM卡ID后20bit) + 位置序号(4bit) + 随机数(4字节)+保留(1字节)
    public byte[] genCloseWriteElsCmd(int psam, byte[] epc, int number, byte[] operator1, byte[] operator2, String dateTime, Error err) {
        byte[] elsCmd = null;
        String dataStr ;
        byte[] data ;
        byte[] dateTimeByte ;
        byte[] numberBytes = new byte[2] ;
        String psamIDStr ;
        byte[] deviceID ;
        byte[] cmdType ;

        numberBytes[0] = (byte) (number/256);
        numberBytes[1] = (byte) (number%256);
        dateTimeByte = dateTimeToByte(dateTime) ;
        psamIDStr = Tools.Bytes2HexString(psamID,psamID.length).substring(3);
        deviceID = Tools.HexString2Bytes(psamIDStr + "0") ;
        dataStr = Tools.Bytes2HexString(numberBytes, 2)
                + Tools.Bytes2HexString(operator1, operator1.length)
                + Tools.Bytes2HexString(operator2, operator2.length)
                + Tools.Bytes2HexString(dateTimeByte, dateTimeByte.length)
                + Tools.Bytes2HexString(deviceID, deviceID.length)
                + "00000000"
                + "00";
        data = Tools.HexString2Bytes(dataStr) ;
        //关锁写物流指令
        cmdType = new byte[]{0x04};
        elsCmd = genElsCmd(psam, cmdType, epc, data, err) ;

        return elsCmd ;
    }


    //恢复指令0x07
    //张数（2字节）+ 操作员1(4字节) + 操作员2(4字节) + 操作时间(4字节) + 手持机ID(PSAM卡ID后20bit) + 位置序号(4bit) + 随机数(4字节)+保留(1字节)
    public byte[] genRecoverElsCmd(int psam, byte[] epc, int number, byte[] operator1, byte[] operator2, String dateTime, Error err) {
        byte[] elsCmd = null;
        String dataStr ;
        byte[] data ;
        byte[] dateTimeByte ;
        byte[] numberBytes = new byte[2] ;
        String psamIDStr ;
        byte[] deviceID ;
        byte[] cmdType ;

        numberBytes[0] = (byte) (number/256);
        numberBytes[1] = (byte) (number%256);
        dateTimeByte = dateTimeToByte(dateTime) ;
        psamIDStr = Tools.Bytes2HexString(psamID,psamID.length).substring(3);
        deviceID = Tools.HexString2Bytes(psamIDStr + "0") ;
        dataStr = Tools.Bytes2HexString(numberBytes, 2)
                + Tools.Bytes2HexString(operator1, operator1.length)
                + Tools.Bytes2HexString(operator2, operator2.length)
                + Tools.Bytes2HexString(dateTimeByte, dateTimeByte.length)
                + Tools.Bytes2HexString(deviceID, deviceID.length)
                + "00000000"
                + "00";
        data = Tools.HexString2Bytes(dataStr) ;
        //关锁写物流指令
        cmdType = new byte[]{0x07};
        elsCmd = genElsCmd(psam, cmdType, epc, data, err) ;

        return elsCmd ;
    }

    //日志清除指令0x08
    //张数（2字节）+ 操作员1(4字节) + 操作员2(4字节) + 操作时间(4字节) + 手持机ID(PSAM卡ID后20bit) + 位置序号(4bit) + 随机数(4字节)+保留(1字节)
    public byte[] genClenLogElsCmd(int psam, byte[] epc, byte[] operator1, byte[] operator2, String dateTime, Error err) {
        byte[] elsCmd = null;
        String dataStr ;
        byte[] data ;
        byte[] dateTimeByte ;
        String psamIDStr ;
        byte[] deviceID ;
        byte[] cmdType ;

        dateTimeByte = dateTimeToByte(dateTime) ;
        psamIDStr = Tools.Bytes2HexString(psamID,psamID.length).substring(3);
        deviceID = Tools.HexString2Bytes(psamIDStr + "0") ;

        dataStr =  Tools.Bytes2HexString(operator1, operator1.length)
                + Tools.Bytes2HexString(operator2, operator2.length)
                + Tools.Bytes2HexString(dateTimeByte, dateTimeByte.length)
                + Tools.Bytes2HexString(deviceID, deviceID.length)
                + "00000000" ;
        data = Tools.HexString2Bytes(dataStr) ;
        //日志清除指令0x08
        cmdType = new byte[]{0x08};
        elsCmd = genElsCmd(psam, cmdType, epc, data, err) ;

        return elsCmd ;
    }

    //追溯指令0x06
    //日志序号(2字节) + 日志项数(1字节) + 结束标识(1字节)
    public byte[] genTraceElsCmd(int psam, byte[] epc, byte[] logId, byte[] logItem, byte[] logFlag , Error err){
        byte[] elsCmd = null;
        byte[] cmdType ;
        String dataStr ;
        byte[] data ;
        cmdType = new byte[]{0x06};
        dataStr = Tools.Bytes2HexString(logId, logId.length)
                + Tools.Bytes2HexString(logItem, logItem.length)
                + Tools.Bytes2HexString(logFlag, logFlag.length) ;
        data = Tools.HexString2Bytes(dataStr) ;
        elsCmd = genElsCmd(psam, cmdType, epc, data, err);
        return elsCmd ;
    }



    //生成电子签封交互指令(GEN_ELS_CMD)
    //8030 + 交互指令 + 00 + 数据长度 + 数据(交互指令DATA)（EPC长度 + EPC + 交互指令数据长度 + 交互指令数据（交互指令 + 随机数））
    public byte[] genElsCmd(int psam, byte[] cmdType, byte[] epc, byte[] data, Error err){
        byte[] elsCmd = null ;
        byte[] result = new byte[256] ;
        String cmdStr ;
        byte[] cmd ;
        byte[] epcLen = {(byte) epc.length};
        byte[] dataLen = {(byte) data.length};
        byte[] allLen = {(byte) (epc.length + data.length + 2)};
        cmdStr = "8030"
                + Tools.Bytes2HexString(cmdType, 1)
                + "00"
                + Tools.Bytes2HexString(allLen, 1)
                + Tools.Bytes2HexString(epcLen, 1)
                + Tools.Bytes2HexString(epc, epc.length)
                + Tools.Bytes2HexString(dataLen, 1)
                + Tools.Bytes2HexString(data, data.length)
                + "00";
        cmd = Tools.HexString2Bytes(cmdStr) ;
        MyLogger.e(TAG, "genElsCmd  send cmd :" + cmdStr);
        int len = rfid.psamapdu(psam, cmd, result) ;
        if (len > 0) {
            String resultStr = Tools.Bytes2HexString(result, len);
            MyLogger.e(TAG, "genElsCmd result :" + resultStr);
            if(resultStr.startsWith("61")){
                //61xx代表后续还xx个数据未接收的
                elsCmd = getResponse(psam, Tools.HexString2Bytes(resultStr.substring(2))) ;
                //PSAM生成的加密
                MyLogger.e(TAG, "genElsCmd finish:" + Tools.Bytes2HexString(elsCmd, elsCmd.length));
            }else{
                err.setErrCode(resultStr);
            }
        }

        return elsCmd ;
    }

    //数据签名(SIGN_DATA), 生成数据签名
    public byte[] genSignData(int psam, byte[] data, Error err){
        byte[] signData = null ;
        byte[] result = new byte[256] ;
        String cmdStr ;
        byte[] cmd ;
        byte[] dataLen = {(byte) data.length};
        cmdStr = "8032"
                + "00"
                + "00"
                + Tools.Bytes2HexString(dataLen, 1)
                + Tools.Bytes2HexString(data, data.length)
                + "00";
        cmd = Tools.HexString2Bytes(cmdStr) ;
        MyLogger.e(TAG, "genSignData  send cmd :" + cmdStr);
        int len = rfid.psamapdu(psam, cmd, result) ;
        if (len > 0) {
            String resultStr = Tools.Bytes2HexString(result, len);
            MyLogger.e(TAG, "genSignData result :" + resultStr);
            if(resultStr.startsWith("61")){
                //61xx代表后续还xx个数据未接收的
                signData = getResponse(psam, Tools.HexString2Bytes(resultStr.substring(2))) ;
                //PSAM生成的加密
                MyLogger.e(TAG, "genSignData finish:" + Tools.Bytes2HexString(signData, signData.length));
            }else{
                err.setErrCode(resultStr);
            }
        }

        return signData ;
    }

    //电子签封数据解密(DECRYPT_ELS_DATA)，只能解析溯源数据
    //8034 00 00 dataLen data
    public byte[] decryptElsData(int psam, byte[] epc, byte[] srcData, Error err) {
        byte[] deData = null ;
        byte[] result = new byte[256] ;
        byte[] epclength = new byte[1];
        byte[] datalength = new byte[1];
        byte[] srcDatalength = new byte[1];
        byte[] cmd ;
        epclength[0] =(byte) (epc.length ) ;
        srcDatalength[0] =(byte) (srcData.length ) ;
        String data = Tools.Bytes2HexString(epclength, 1)
                + Tools.Bytes2HexString(epc, epc.length)
                + Tools.Bytes2HexString(srcDatalength, 1)
                + Tools.Bytes2HexString(srcData, srcData.length) ;

        datalength[0] =(byte) (data.length()/2 ) ;
        String cmdStr = "80340600" + Tools.Bytes2HexString(datalength, 1) + data + "00";
        MyLogger.e(TAG, "decryptElsData  send cmd :" + cmdStr);
        cmd = Tools.HexString2Bytes(cmdStr) ;
        int len = rfid.psamapdu(psam, cmd, result);
        if (len > 0) {
            String resultStr = Tools.Bytes2HexString(result, len) ;
            MyLogger.e(TAG, "decryptElsData :" + resultStr);
            if(resultStr.startsWith("61")){
                //61xx代表后续还xx个数据未接收的
                deData=  getResponse(psam, Tools.HexString2Bytes(resultStr.substring(2))) ;
//                MyLogger.e(TAG, "getRFIDPassword result:" + Tools.Bytes2HexString(result, len));
            }else{
                err.setErrCode(resultStr);
            }
        }
        return deData ;
    }

    //获取 RFID 访问密码(GET_RFID_PWD)   80360000 + 数据长度 + EPC数据 + 00
    public byte[] getRFIDPassword(int psam, byte[] epc, Error err){
        byte[] result = new byte[256] ;
        byte[] pwd = new byte[32] ;
        byte[] length = new byte[1];
        length[0] =(byte) (epc.length ) ;
        String cmdStr = "80360000" + Tools.Bytes2HexString(length, 1) + Tools.Bytes2HexString(epc, epc.length) + "04" ;
        byte[] cmd = Tools.HexString2Bytes(cmdStr) ;
        //80360000 + 数据长度 + EPC数据 + 00
        int len = rfid.psamapdu(psam, cmd, result);
        if (len > 0) {
            String resultStr = Tools.Bytes2HexString(result, len) ;
            MyLogger.e(TAG, "getRFIDPassword :" + resultStr);
            if(resultStr.startsWith("61")){
                //61xx代表后续还xx个数据未接收的
                pwd = getResponse(psam, Tools.HexString2Bytes(resultStr.substring(2))) ;
                MyLogger.e(TAG, "getRFIDPassword result:" + Tools.Bytes2HexString(result, len));
            }else{
                err.setErrCode(resultStr);
            }

        }
        return pwd ;
    }


    //测试EPC:481F2281 11 4E20 3E 00D FA8 40
    //用户PIN码：481F2281114E203E00DFA840


    //获取后续响应
    public byte[] getResponse(int psam, byte[] length){
        byte[] result = new byte[256] ;
        byte[] resByte = null;
        byte[] cmd  = Tools.HexString2Bytes("00C00000" + Tools.Bytes2HexString(length, length.length)) ;
        MyLogger.e(TAG, "getResponsecmd=" + "00C00000" + Tools.Bytes2HexString(length, length.length));
        int len = rfid.psamapdu(psam, cmd, result);
        if (len > 0) {
            String res = Tools.Bytes2HexString(result, len) ;
            if(res.endsWith("9000")){
                res = res.substring(0, res.length() - 4) ;
                resByte = Tools.HexString2Bytes(res) ;
            }
            MyLogger.e(TAG, "getResponse :" + Tools.Bytes2HexString(result, len));
        }
        return resByte ;
    }


    /* 日期字符串转字节 */
    public static byte[] dateTimeToByte(String dateStr) {
        Long time = 0L ;
        long year = 0 ;
        long month = 0 ;
        long day = 0 ;
        long hour = 0 ;
        long minute = 0 ;
        long second = 0 ;
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = simpleDateFormat.parse(dateStr);
            year = date.getYear()%100 ;
            month = (date.getMonth() + 1) ;
            day = date.getDay() ;
            hour = date.getHours() ;
            minute = date.getMinutes() ;
            second = date.getSeconds() ;
        }catch (Exception e){

        }

        //将时间转成4个字节
        //2019-4-4 16:52:21   4D090D15
        time = ((year << 26) | (month << 22) | (day << 17) | (hour << 12) | (minute << 6) | second);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(time) ;
        byte[] t = buffer.array() ;
        byte[] result = new byte[4] ;
        System.arraycopy(t, 4, result, 0, 4);
        return result ;
    }
}
