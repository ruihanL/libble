package com.xs.libwc;

import java.util.ArrayList;

/**
 * Created by linrh on 2016/8/26.
 */
public class WC {

    public static ArrayList<String> zljh = new ArrayList<>();

    static {
        zljh.add("ZTCX");
        zljh.add("ZTYD");
        zljh.add("ZTHB");
        zljh.add("HBYD");
        zljh.add("SJSZ");
        zljh.add("SJYD");
    }

    private byte[] src = null;
    private char bz ='$';
    private String zlm = null;
    private int len = 0;
    private byte[] detail = null;
    private byte crc;

    private Boolean isValid = false;

    public WC(byte[] src) {
        this.src = src;
        //长度符合规定，而且第一个字符是$
        if (src!=null&&src.length>=7&&src.length<=20&&src[0]==(byte)bz&&src[5]==src.length&&checkCRC(src)){
            this.len = src[5];
            byte[] zlm = new byte[4];
            for (int i= 0;i<4;i++){
                zlm[i] = src[i+1];
            }
            String zl = new String(zlm);
            if (zljh.contains(zl)){
                this.zlm = zl;
                int detail_len = src[5]-7;
                if (detail_len>0){
                    this.detail = new byte[detail_len];
                    for (int i= 0;i<detail_len;i++){
                        this.detail[i] = src[i+6];
                    }

                    this.crc = src[len-1];
                    this.isValid = true;
                }
            }
        }
    }

    public Boolean getValid() {
        return isValid;
    }

    public Boolean analyse(WClistener listener){

        if (getValid()&&listener!=null){
            if (zlm!=null)
            {
                if (zlm.equals("ZTYD")){
                    int p = detail[0];
                    int a = detail[1];
                    listener.ZTYD(p,a);

                }else if (zlm.equals("ZTHB")){
                    int p = detail[0];
                    int a = detail[1];
                    listener.ZTHB(p,a);

                    //此时需要应答终端
                    HBYD hbyd = new HBYD();
                    listener.YD(hbyd.getSrc());

                }else if (zlm.equals("SJYD")){
                    int y = detail[0];
                    int m = detail[1];
                    int d = detail[2];
                    int h = detail[3];
                    int f = detail[4];
                    int s = detail[5];
                    listener.SJYD(y,m,d,h,f,s);
                }
            }
            return true;
        }
        return false;
    }

    private static Boolean checkCRC(byte[] data) {
        byte crc_val = 0;

        for (int i = 0; i < data.length-1; i++) {
            crc_val = (byte) (crc_val ^ data[i]);
        }

        return crc_val == data[data.length - 1] ? true:false;
    }

    public static byte getCrc(byte[] data, int len) {
        byte crc_val = 0;

        for (int i = 0; i < len; i++) {
            crc_val = (byte) (crc_val ^ data[i]);
        }

        return crc_val ;
    }

    public interface WClistener{
        void ZTYD(int power,int alarm);
        void ZTHB(int power,int alarm);
        void SJYD(int year,int month,int day,int hour,int min,int second);
        void YD(byte[] src);
    }
}
