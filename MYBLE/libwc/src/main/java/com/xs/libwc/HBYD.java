package com.xs.libwc;

/**
 * Created by linrh on 2016/8/26.
 */
public class HBYD {
    public static char bz = '$';
    public static String zlm = "HBYD";
    public int len = 7;



    public HBYD() {

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

        s[6] = WC.getCrc(s,len-1);
        return s;
    }
}
