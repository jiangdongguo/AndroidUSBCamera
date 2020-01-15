package org.easydarwin.sw;

/**
 */
public class JNIUtil {

    static {
        System.loadLibrary("Utils");
    }

    /**
     * 都是Y：U：V = 4：1：1但 U与 V顺序相反。变换可逆
     *
     * @param buffer
     * @param width
     * @param height
     */
    public static void yV12ToYUV420P(byte[] buffer, int width, int height) {
        callMethod("YV12ToYUV420P", null, buffer, width, height);
    }

    /**
     * 都是Y：U+V = 4：2,但是这两者U、V方向相反。变换可逆
     *
     * @param buffer
     * @param width
     * @param height
     */
    public static void nV21To420SP(byte[] buffer, int width, int height) {
        callMethod("NV21To420SP", null, buffer, width, height);
    }

    /**
     * 旋转1个字节为单位的矩阵
     *
     * @param data   要旋转的矩阵
     * @param offset 偏移量
     * @param width  宽度
     * @param height 高度
     * @param degree 旋转度数
     */
    public static void rotateMatrix(byte[] data, int offset, int width, int height, int degree) {
        callMethod("RotateByteMatrix", null, data, offset, width, height, degree);
    }

    /**
     * 旋转2个字节为单位的矩阵
     *
     * @param data   要旋转的矩阵
     * @param offset 偏移量
     * @param width  宽度
     * @param height 高度
     * @param degree 旋转度数
     */
    public static void rotateShortMatrix(byte[] data, int offset, int width, int height, int degree) {
        callMethod("RotateShortMatrix", null, data, offset, width, height, degree);
    }

    private static native void callMethod(String methodName, Object[] returnValue, Object... params);

}
