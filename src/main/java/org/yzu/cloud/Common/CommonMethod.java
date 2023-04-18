package org.yzu.cloud.Common;

import com.sun.jna.Pointer;

import java.io.UnsupportedEncodingException;

public class CommonMethod {
    public static void WriteBuffToPointer(byte[] byData, Pointer pInBuffer){
        pInBuffer.write(0, byData, 0, byData.length);
    }

    public static String byteToString(byte[] bytes) {
        if (null == bytes || bytes.length == 0) {
            return "";
        }
        int iLengthOfBytes = 0;
        for(byte st:bytes){
            if(st != 0){
                iLengthOfBytes++;
            }else
                break;
        }
        String strContent = "";
        try {
            strContent = new String(bytes, 0, iLengthOfBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return strContent;
    }
}
