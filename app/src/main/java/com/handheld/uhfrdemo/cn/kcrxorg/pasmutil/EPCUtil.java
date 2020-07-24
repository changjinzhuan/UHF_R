package com.handheld.uhfrdemo.cn.kcrxorg.pasmutil;

public class EPCUtil {
    //EPC
    public byte[] epc ;
    //解析EPC
    public EPCUtil(byte[] epc) {
        this.epc = epc ;
    }

    public byte[] getTagID() {
        byte[] id = new byte[4] ;
        if (epc.length > 4) {
            System.arraycopy(epc, 0, id, 0, 4);
        }
        return id ;
    }

    //EPC 中的随机数
    public byte[] getRandom() {
        byte[] random = new byte[1] ;
        if(epc.length > 8){
            random[0] = epc[7] ;
        }
        return random ;
    }

}
