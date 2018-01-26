package com.jiangdg.usbcamera.utils;

/** NV21数据类
 *
 * Created by jiangdongguo on 2018/1/26.
 */

public class YUVBean {
    private int width;
    private int height;
    private byte[] yuvData;
    private String picPath;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public byte[] getYuvData() {
        return yuvData;
    }

    public void setYuvData(byte[] yuvData) {
        this.yuvData = yuvData;
    }

    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }
}
