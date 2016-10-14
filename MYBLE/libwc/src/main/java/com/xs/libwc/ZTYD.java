package com.xs.libwc;

/**
 * Created by linrh on 2016/8/26.
 *
 */
public class ZTYD {
    public static char bz = '$';
    public static String zlm = "ZTYD";
    public int len = 9;
    public int power = 0;
    public int alarm = 0;


    public ZTYD(int alarm, int power) {
        this.alarm = alarm;
        this.power = power;
    }

    public byte[] getSrc()
    {
        byte[] s = new byte[len];
        s[0] = (byte)bz;
        byte[] z = zlm.getBytes();
        for (int i = 0;i<z.length;i++){
            s[i+1] = z[i];
        }
        s[5] = (byte)len;
        s[6] = (byte)power;
        s[7] = (byte)alarm;
        s[8] = WC.getCrc(s,len-1);
        return s;
    }
}
