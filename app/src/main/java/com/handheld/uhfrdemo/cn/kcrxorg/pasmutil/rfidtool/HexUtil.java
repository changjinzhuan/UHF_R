package com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.rfidtool;


//字节的转换

public class HexUtil {

 //将字节数组转换为short类型，即统计字符串长度

	public static short bytes2Short2(byte[] b) {

		short i = (short) (((b[1] & 0xff) << 8) | b[0] & 0xff);

		return i;

	}
	public static String addZeroLeft(String s, int len)
	{
		while(s.length()<len)
		{
			s="0"+s;
		}
		return s;
	}
	public static byte[] intToBytes(int value)
	{
		byte[] src = new byte[4];
		src[0] = (byte)((value >> 24) & 0xFF);
		src[1] = (byte)((value >> 16) & 0xFF);
		src[2] = (byte)((value >> 8) & 0xFF);
		src[3] = (byte)(value & 0xFF);
		return src;
	}
	
	public static String bytesToHexString(byte[] src){   
	    StringBuilder stringBuilder = new StringBuilder("");   
	    if (src == null || src.length <= 0) {   
	        return null;   
	    }   
	    for (int i = 0; i < src.length; i++) {   
	        int v = src[i] & 0xFF;   
	        String hv = Integer.toHexString(v);   
	        if (hv.length() < 2) {   
	            stringBuilder.append(0);   
	        }   
	        stringBuilder.append(hv);   
	    }   
	    return stringBuilder.toString();   
	}   
	public static byte[] hexStringToBytes(String hexString) {   
	    if (hexString == null || hexString.equals("")) {   
	        return null;   
	    }   
	    hexString = hexString.toUpperCase();   
	    int length = hexString.length() / 2;   
	    char[] hexChars = hexString.toCharArray();   
	    byte[] d = new byte[length];   
	    for (int i = 0; i < length; i++) {   
	        int pos = i * 2;   
	        d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));   
	    }   
	    return d;   
	}   
	
	private static byte charToByte(char c) {   
	    return (byte) "0123456789ABCDEF".indexOf(c);   
	}  

}
