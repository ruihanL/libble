package com.xs.libble;
import java.util.UUID;

/**
 * 硬件相关配置
 */
public class Dev {
    /**
     * 蓝牙模块UUID配置。此为固定配置，根据蓝牙模块实际UUID进行配置。
     */
    public static  UUID SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");//GATT Service
    public static  UUID NOTIFY  = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");//read characteristic
    public static  UUID WRITE   = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");//write characteristic
    public static  UUID CONFIG  = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");//config derator

    public static Boolean isRWsame = false;
    public static Boolean needConfig = true;

    public static void setCONFIG(UUID CONFIG) {
        Dev.CONFIG = CONFIG;
    }

    public static void setNOTIFY(UUID NOTIFY) {
        Dev.NOTIFY = NOTIFY;
    }

    public static void setSERVICE(UUID SERVICE) {
        Dev.SERVICE = SERVICE;
    }

    public static void setWRITE(UUID WRITE) {
        Dev.WRITE = WRITE;
    }
}
