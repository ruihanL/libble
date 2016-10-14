package com.xs.libwc;

/**
 * Created by linrh on 2016/8/26.
 */
public class SJYD {
    public static char bz = '$';
    public static String zlm = "SJYD";
    public int len = 13;
    public int year = 0;
    public int month = 1;
    public int day = 1;
    public int hour = 0;
    public int min = 0;
    public int second = 0;


    public SJYD(int year, int month, int day, int hour, int min , int second) {
        this.day = day;
        this.hour = hour;
        this.min = min;
        this.month = month;
        this.second = second;
        this.year = year;
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
        s[6] = (byte)year;
        s[7] = (byte)month;
        s[8] = (byte)day;
        s[9] = (byte)hour;
        s[10] = (byte)min;
        s[11] = (byte)second;
        s[12] = WC.getCrc(s,len-1);
        return s;
    }
}
