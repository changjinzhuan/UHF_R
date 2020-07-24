package com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.rfidtool;

import android.util.Log;

import com.BRMicro.Tools;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.cn.kcrx.bean.TagEpcData;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.cn.kcrx.bean.TagUserdata;
import com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.cn.kcrx.bean.UserTraceData;

import java.text.SimpleDateFormat;

public class UserReader {
    public static TagUserdata readTagUser(byte[] userdata)
    {
        TagUserdata tagUserdata=new TagUserdata();
        try
        {
            String userdatastr = Tools.Bytes2HexString(userdata, userdata.length);
          //  if (userdatastr.length() != 432)
            if (userdatastr.length() != 432)
            {
                return tagUserdata;
            }
            String alluserTraceDatastr = userdatastr.substring(0, 256);//user区物流数据
            UserTraceData[] userTraceDatas = new UserTraceData[8];

            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < alluserTraceDatastr.length() / 32; i++)
            {
                String userTraceDatastr = alluserTraceDatastr.substring(32 * i, 32 * i+32);
                if(!userTraceDatastr.startsWith("B"))//如果不是开关锁指令，不读取
                {
                    Log.d("userreader","不是开关锁物流指令，不读取:"+userTraceDatastr);
                    UserTraceData b=new UserTraceData();
                    b.setCommandid(userTraceDatastr.substring(0, 2));
                    userTraceDatas[i]=b;
                    continue;
                }
                UserTraceData userTraceData = new UserTraceData();
                userTraceData.setCommandid(userTraceDatastr.substring(0, 2));
                userTraceData.setOperator1(userTraceDatastr.substring(2, 10));
                userTraceData.setOperator2(userTraceDatastr.substring(10, 18));
               // byte[] datetimeB = Tools.HexString2Bytes(userTraceDatastr.substring(18, 8));
             //   Log.d("userreader","datetimestr="+userTraceDatastr.substring(18, 26));
                int datetimeint=Integer.parseInt(userTraceDatastr.substring(18, 26),16);
                long year = (datetimeint & 0xFC000000) >> 26;

                //Debug.WriteLine("year===" + year);
                long month = (datetimeint & 0x03C00000) >> 22;

                //Debug.WriteLine("month===" + month);
                long day = (datetimeint & 0x003E0000) >> 17;
                //Debug.WriteLine("day===" + day);
                long hour = (datetimeint & 0x0001F000) >> 12;
                //Debug.WriteLine("hour===" + hour);
                long minute = (datetimeint & 0x00000FC0) >> 6;
                //Debug.WriteLine("minute===" + minute);
                long second = (datetimeint & 0x0000003F);
                if (month < 1)
                {
                    userTraceData.setOpdatetime("1900-01-01 00:00:00");
                }
                else
                {
                    userTraceData.setOpdatetime(year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second);
                }
                userTraceDatas[i] = userTraceData;
               // System.out.println("第" + i + "条user区物流数据:命令为" + userTraceDatas[i].getCommandid() + " 操作人:" + userTraceDatas[i].getOperator1() + " 复核人:" + userTraceDatas[i].getOperator2() + " 操作时间:" + userTraceDatas[i].getOpdatetime());
            }
            tagUserdata.setUserTraceData(userTraceDatas);
            String epcdatabakstr = userdatastr.substring(256, 288);//Epc备份区
            TagEpcData tagEpcData = EpcReader.readEpc(epcdatabakstr.substring(0, 24));//只取前12byte作为EPC
            tagUserdata.setTagEpcDatabak(tagEpcData);

             Log.e("UserReader","硬件版本为:" + userdatastr.substring(288, 289));
             Log.e("UserReader","软件版本为:" + userdatastr.substring(289, 292));
             Log.e("UserReader","日志计数为:" + userdatastr.substring(292, 296));
            tagUserdata.setHardwareVersion(userdatastr.substring(288, 289));
            tagUserdata.setSoftVersion(userdatastr.substring(289, 292));
            tagUserdata.setLogCount(userdatastr.substring(292, 296));
            //32状态位
            String stat32usstr=userdatastr.substring(296,304);
            Log.e("UserReader","statu32str="+stat32usstr);
            int status=Integer.parseInt(stat32usstr,16);
           // stat32usstr=String.format("%032d",Integer.parseInt(Integer.toBinaryString(status)));;
            stat32usstr=addZeroLeft(Integer.toBinaryString(status),32);
            Log.e("UserReader","statu32BinaryString="+stat32usstr);
            char[] cs=stat32usstr.toCharArray();
            Boolean[] booleans=new Boolean[cs.length];
            for(int i=0;i<booleans.length;i++)
            {
                if(cs[i]=='1')
                {
                    booleans[i]=true;
                }else
                {
                    booleans[i]=false;
                }
            }
            tagUserdata.setUserErrorData(booleans);

        }catch (Exception e)
        {
            Log.e("UserReader","User区读取失败:"+e.getLocalizedMessage());
            return tagUserdata;
        }
        return tagUserdata;
    }
    //B400000000000000004D3106F8EB0000B4000313600000000046EEDEA97B0000B300000000000000004D31094F120000B400000000000000004D310CCEED0000B300000000000000004D30EDD7690000B400000000000000004D30EE4EC80000B3000313600003135E3B20A325540000B300000000000000004D310653B700003B9B5A0E1100003801B0883E000000001032002000000000BB006854B23BDC655D96CB8885AED378BB1EBCBB233A0E30D7E70049391FE89D80A960DA5C6F2FBAA7E50000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

    public static void main(String[] args)
    {
        String userstr="B3000310DC000310DD48AF2287270000B300000000000000004ECAB2E6F90000B400000000000000004ECAB3CAF9000000000000000000000000000000000000B300000000000000004ECAB560A60000B400000000000000004ECB0BBAA20000B300000000000000004ECB0C73800000000000000000000000000000000000003B9CD92A114E200C09C2A4AF000000001032009E00080000BB00000039F7027783220BB3348BD12226E8CDA5F27B0DE7F8D618A850D709759847F81A97E50F6F584C00000000000000000000000000000000000000000000";
        readTagUser(Tools.HexString2Bytes(userstr));
    }
    public static String addZeroLeft(String s, int len)
    {
        while(s.length()<len)
        {
            s="0"+s;
        }
        return s;
    }
}
