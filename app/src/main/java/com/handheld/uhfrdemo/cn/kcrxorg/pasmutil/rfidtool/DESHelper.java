package com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.rfidtool;
import java.security.Security;

import java.util.Arrays;

 

import javax.crypto.Cipher;

import javax.crypto.SecretKey;

import javax.crypto.spec.IvParameterSpec;

import javax.crypto.spec.SecretKeySpec;

 

//import org.bouncycastle.jce.provider.BouncyCastleProvider;

//import org.bouncycastle.util.encoders.Hex;

 

/***

 * 可以和c#加密结果相同     需要到两个jar包：

 * bcprov-jdk15on-148.jar

 * bcprov-ext-jdk15on-148.jar   在D:/jar/

 * 

 * @ClassName Test3DES_2

 * @author 张月

 * @date 2014年4月16日

 */

public class DESHelper {

	

	public static void main(String[] args) throws Exception {

		String key = "74313E65431691814EEC93ECF2611DD1";

		String shuju = "3084A06E23C2D207";

		String jiamiResult = encryptStr(shuju,key);

		System.out.println("加密后："+jiamiResult);//【与C# CBC 零填充模式 互相加解密】

		

		System.out.println("解密后："+decryptStr(jiamiResult,key));

	}

	

	/**

	 * 3DES加密 (亦称为：DESede加密)  

	 * 

	 * CBC模式

	 * 填充模式：零字节填充  ZeroBytePadding

	 * 

	 * @Method: encrypt3Str 

	 * @param @param shuju

	 * @param @param key

	 * @param @return

	 * @param @throws Exception

	 * @return String

	 * @throws

	 */

	public static String encryptStr(String shuju,String key) throws Exception {

		String result = "";

		try {

			//Security.addProvider(new BouncyCastleProvider());
			byte[] bKey = HexUtil.hexStringToBytes(key);//十六进制转换成字节数据

			byte[] bMsg = HexUtil.hexStringToBytes(shuju);

 

			byte[] keyBytes = Arrays.copyOf(bKey, 24);

			int j = 0, k = 16;

			while (j < 8) {

			    keyBytes[k++] = keyBytes[j++];

			}

 

			SecretKey key3 = new SecretKeySpec(keyBytes, "DESede");

			IvParameterSpec iv3 = new IvParameterSpec(new byte[8]);//初始向量默认 0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00

			Cipher cipher3 = Cipher.getInstance("DESede/CBC/PKCS5Padding");

			cipher3.init(Cipher.ENCRYPT_MODE, key3, iv3);

 

			byte[] bMac = cipher3.doFinal(bMsg);

			result=  HexUtil.bytesToHexString(bMac);//encode方法字节数组转换成十六进制
			System.out.println("result"+result);

		} catch (Exception e) {

			e.printStackTrace();

			throw e;

		}

		return result;

	}

	

	/**

	 * 3DES 解密

	 * @Method: decryptStr 

	 * @param @param shuju

	 * @param @param key

	 * @param @return

	 * @param @throws Exception

	 * @return String

	 * @throws

	 */

	public static String decryptStr(String shuju,String key) throws Exception{

		String result = "";

		try {

			//Security.addProvider(new BouncyCastleProvider());

			byte[] bKey = HexUtil.hexStringToBytes(key);//十六进制转换成字节数据

			byte[] bMsg = HexUtil.hexStringToBytes(shuju);

 

			byte[] keyBytes = Arrays.copyOf(bKey, 24);

			int j = 0, k = 16;

			while (j < 8) {

			    keyBytes[k++] = keyBytes[j++];

			}

 

			SecretKey key3 = new SecretKeySpec(keyBytes, "DESede");

			IvParameterSpec iv3 = new IvParameterSpec(new byte[8]);//初始向量默认 0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00

			Cipher cipher3 = Cipher.getInstance("DESede/CBC/PKCS5Padding");
		//	Cipher cipher3 = Cipher.getInstance("DES/CBC/PKCS5Padding");
			cipher3.init(Cipher.DECRYPT_MODE, key3, iv3);

			byte[] bMac = cipher3.doFinal(bMsg);

			result= HexUtil.bytesToHexString(bMac);//encode方法字节数组转换成十六进制

		} catch (Exception e) {

			e.printStackTrace();

			throw e;

		}

		return result;

	} 

}
