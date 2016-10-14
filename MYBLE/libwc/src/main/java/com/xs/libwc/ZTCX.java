package com.xs.libwc;

/**
 * Created by linrh on 2016/8/26.
 * 终端状态查询
 使用场景：该命令用于主机主动查询终端的运行状态信息，包括电量，报警情况等。
 命令流向：主机->终端
 标    识：$
 指 令 码：ZTCX （注：即状态查询）
 长    度：7
 消 息 体：空
 检 验 码：
 返    回：终端返回见“终端状态应答”。

 */
public class ZTCX {
    public static char bz = '$';
    public static String zlm = "ZTCX";
    public int len = 7;

    public ZTCX() {

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
