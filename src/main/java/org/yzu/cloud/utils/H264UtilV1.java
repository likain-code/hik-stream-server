package org.yzu.cloud.utils;

import org.yzu.cloud.websocket.WebsocketImpl;

import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;

public class H264UtilV1 {

    private static byte[] allEsBytes = null;

    /**
     * 提取H264裸流
     *
     * @param outputData
     * @throws IOException
     */
    public static void writeESH264(final byte[] outputData) throws IOException {
        if (outputData.length == 0) {
            return;
        }
        if ((outputData[0] & 0xff) == 0x00//
                && (outputData[1] & 0xff) == 0x00//
                && (outputData[2] & 0xff) == 0x01//
                && (outputData[3] & 0xff) == 0xBA) {// RTP包开头
            try {
                // 一个完整的帧解析完成后将解析的数据放入BlockingQueue,websocket获取后发生给前端
                if (allEsBytes != null && allEsBytes.length > 0) {
                    for (Session client : WebsocketImpl.CLIENTS) {
                        client.getBasicRemote().sendBinary(ByteBuffer.wrap(allEsBytes));
                    }
                }
                allEsBytes = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 是00 00 01 eo开头的就是视频的pes包
        if ((outputData[0] & 0xff) == 0x00//
                && (outputData[1] & 0xff) == 0x00//
                && (outputData[2] & 0xff) == 0x01//
                && (outputData[3] & 0xff) == 0xE0) {//
            // 去掉包头后的起始位置
            int from = 9 + outputData[8] & 0xff;
            int len = outputData.length - 9 - (outputData[8] & 0xff);
            // 获取es裸流
            byte[] esBytes = new byte[len];
            System.arraycopy(outputData, from, esBytes, 0, len);

            if (allEsBytes == null) {
                allEsBytes = esBytes;
            } else {
                byte[] newEsBytes = new byte[allEsBytes.length + esBytes.length];
                System.arraycopy(allEsBytes, 0, newEsBytes, 0, allEsBytes.length);
                System.arraycopy(esBytes, 0, newEsBytes, allEsBytes.length, esBytes.length);
                allEsBytes = newEsBytes;
            }
        }
    }
}
