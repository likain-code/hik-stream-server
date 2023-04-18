package org.yzu.cloud.StreamService;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.yzu.cloud.AlarmService.HCISUPAlarm;
import org.yzu.cloud.CmsService.CmsDemo;
import org.yzu.cloud.CmsService.HCISUPCMS;
import org.yzu.cloud.Common.PropertiesUtil;
import org.yzu.cloud.Common.osSelect;
import org.yzu.cloud.utils.H264Util;
import org.yzu.cloud.utils.H264UtilV1;
import org.yzu.cloud.websocket.WebsocketImpl;

import javax.websocket.Session;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

public class StreamDemo {

    public static byte[] allEsBytes = null;
    public static HCISUPStream hCEhomeStream = null;
    public static PlayCtrl playCtrl = null;
    static int m_lPlayBackLinkHandle = -1;   //回放句柄
    public static int m_lPlayBackListenHandle = -1; //回放监听句柄

    public static int StreamHandle = -1;   //预览监听句柄
    public static int lPreviewHandle = -1; //
    public static int sessionID = -1; //预览sessionID
    static int backSessionID = -1;  //回放sessionID
    static int Count = 0;
    static int iCount = 0;
    static IntByReference m_lPort = new IntByReference(-1);//回调预览时播放库端口指针
    static File file = new File("/home/realPlayData.mp4");
    static FileOutputStream m_file = null;
    static String configPath = "/home/config.properties";
    PropertiesUtil propertiesUtil;

    {
        try {
            propertiesUtil = new PropertiesUtil(configPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    HCISUPStream.NET_EHOME_PLAYBACK_LISTEN_PARAM struPlayBackListen = new HCISUPStream.NET_EHOME_PLAYBACK_LISTEN_PARAM();
    HCISUPAlarm.NET_EHOME_ALARM_LISTEN_PARAM net_ehome_alarm_listen_param = new HCISUPAlarm.NET_EHOME_ALARM_LISTEN_PARAM();
    HCISUPStream.NET_EHOME_LISTEN_PREVIEW_CFG struPreviewListen = new HCISUPStream.NET_EHOME_LISTEN_PREVIEW_CFG();
    static FPREVIEW_NEWLINK_CB fPREVIEW_NEWLINK_CB;//预览监听回调函数实现
    static FPREVIEW_DATA_CB fPREVIEW_DATA_CB;//预览回调函数实现
    static PLAYBACK_NEWLINK_CB fPLAYBACK_NEWLINK_CB; //回放监听回调函数实现
    static PLAYBACK_DATA_CB fPLAYBACK_DATA_CB;   //回放回调实现

    /**
     * 动态库加载
     *
     * @return
     */
    private static boolean CreateSDKInstance() {
        if (hCEhomeStream == null) {
            synchronized (HCISUPStream.class) {
                String strDllPath = "";
                try {
                    if (osSelect.isWindows())
                        //win系统加载库路径
                        strDllPath = System.getProperty("user.dir") + "\\lib\\HCISUPStream.dll";

                    else if (osSelect.isLinux())
                        //Linux系统加载库路径
                        strDllPath = System.getProperty("user.dir") + "/lib/libHCISUPStream.so";
                    hCEhomeStream = (HCISUPStream) Native.loadLibrary(strDllPath, HCISUPStream.class);
                } catch (Exception ex) {
                    System.out.println("loadLibrary: " + strDllPath + " Error: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    public void eStream_Init() {
        if (hCEhomeStream == null) {
            if (!CreateSDKInstance()) {
                System.out.println("Load Stream SDK fail");
                return;
            }
        }
//        if (playCtrl == null) {
//            if (!CreatePlayInstance()) {
//                System.out.println("Load PlayCtrl fail");
//                return;
//            }
//        }
        if (osSelect.isWindows()) {
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCrypto = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCrypto = System.getProperty("user.dir") + "\\lib\\libeay32.dll"; //Linux版本是libcrypto.so库文件的路径
            System.arraycopy(strPathCrypto.getBytes(), 0, ptrByteArrayCrypto.byValue, 0, strPathCrypto.length());
            ptrByteArrayCrypto.write();
            if (!hCEhomeStream.NET_ESTREAM_SetSDKInitCfg(0, ptrByteArrayCrypto.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 0 failed, error:" + hCEhomeStream.NET_ESTREAM_GetLastError());
            }
            HCISUPCMS.BYTE_ARRAY ptrByteArraySsl = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathSsl = System.getProperty("user.dir") + "\\lib\\ssleay32.dll";    //Linux版本是libssl.so库文件的路径
            System.arraycopy(strPathSsl.getBytes(), 0, ptrByteArraySsl.byValue, 0, strPathSsl.length());
            ptrByteArraySsl.write();
            if (!hCEhomeStream.NET_ESTREAM_SetSDKInitCfg(1, ptrByteArraySsl.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 1 failed, error:" + hCEhomeStream.NET_ESTREAM_GetLastError());
            }
            //流媒体初始化
            hCEhomeStream.NET_ESTREAM_Init();
            //设置HCAapSDKCom组件库文件夹所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCom = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCom = System.getProperty("user.dir") + "\\lib\\HCAapSDKCom";      //只支持绝对路径，建议使用英文路径
            System.arraycopy(strPathCom.getBytes(), 0, ptrByteArrayCom.byValue, 0, strPathCom.length());
            ptrByteArrayCom.write();
            if (!hCEhomeStream.NET_ESTREAM_SetSDKLocalCfg(5, ptrByteArrayCom.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKLocalCfg 5 failed, error:" + hCEhomeStream.NET_ESTREAM_GetLastError());
            }
            hCEhomeStream.NET_ESTREAM_SetLogToFile(3, "..\\EHomeSDKLog", false);
        } else if (osSelect.isLinux()) {
            //设置libcrypto.so所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCrypto = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCrypto = System.getProperty("user.dir") + "/lib/libcrypto.so"; //Linux版本是libcrypto.so库文件的路径
            System.arraycopy(strPathCrypto.getBytes(), 0, ptrByteArrayCrypto.byValue, 0, strPathCrypto.length());
            ptrByteArrayCrypto.write();
            if (!hCEhomeStream.NET_ESTREAM_SetSDKInitCfg(0, ptrByteArrayCrypto.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 0 failed, error:" + hCEhomeStream.NET_ESTREAM_GetLastError());
            }
            //设置libssl.so所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArraySsl = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathSsl = System.getProperty("user.dir") + "/lib/libssl.so";    //Linux版本是libssl.so库文件的路径
            System.arraycopy(strPathSsl.getBytes(), 0, ptrByteArraySsl.byValue, 0, strPathSsl.length());
            ptrByteArraySsl.write();
            if (!hCEhomeStream.NET_ESTREAM_SetSDKInitCfg(1, ptrByteArraySsl.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 1 failed, error:" + hCEhomeStream.NET_ESTREAM_GetLastError());
            }
            hCEhomeStream.NET_ESTREAM_Init();
            //设置HCAapSDKCom组件库文件夹所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCom = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCom = System.getProperty("user.dir") + "/lib/HCAapSDKCom/";      //只支持绝对路径，建议使用英文路径
            System.arraycopy(strPathCom.getBytes(), 0, ptrByteArrayCom.byValue, 0, strPathCom.length());
            ptrByteArrayCom.write();
            if (!hCEhomeStream.NET_ESTREAM_SetSDKLocalCfg(5, ptrByteArrayCom.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKLocalCfg 5 failed, error:" + hCEhomeStream.NET_ESTREAM_GetLastError());
            }
            hCEhomeStream.NET_ESTREAM_SetLogToFile(3, "./EHomeSDKLog", false);
        }

    }

    public void startRealPlayListen() {
        //预览监听
        if (fPREVIEW_NEWLINK_CB == null) {
            fPREVIEW_NEWLINK_CB = new FPREVIEW_NEWLINK_CB();
        }
        System.arraycopy(propertiesUtil.readValue("SmsServerListenIP").getBytes(), 0, struPreviewListen.struIPAdress.szIP, 0, propertiesUtil.readValue("SmsServerListenIP").length());
        struPreviewListen.struIPAdress.wPort = Short.parseShort(propertiesUtil.readValue("SmsServerListenPort")); //流媒体服务器监听端口
        struPreviewListen.fnNewLinkCB = fPREVIEW_NEWLINK_CB; //预览连接请求回调函数
        struPreviewListen.pUser = null;
        struPreviewListen.byLinkMode = 0; //0- TCP方式，1- UDP方式
        struPreviewListen.write();

        if (StreamHandle < 0) {
            StreamHandle = hCEhomeStream.NET_ESTREAM_StartListenPreview(struPreviewListen);
            if (StreamHandle == -1) {

                System.out.println("NET_ESTREAM_StartListenPreview failed, error code:" + hCEhomeStream.NET_ESTREAM_GetLastError());
                hCEhomeStream.NET_ESTREAM_Fini();
                return;
            } else {
                String StreamListenInfo = new String(struPreviewListen.struIPAdress.szIP).trim() + "_" + struPreviewListen.struIPAdress.wPort;
                System.out.println("流媒体服务：" + StreamListenInfo + ",NET_ESTREAM_StartListenPreview succeed");
            }
        }
    }

    public void startPlayBackListen() {
        //回放监听
        if (fPLAYBACK_NEWLINK_CB == null) {
            fPLAYBACK_NEWLINK_CB = new PLAYBACK_NEWLINK_CB();
        }
        System.arraycopy(propertiesUtil.readValue("SmsBackServerListenIP").getBytes(), 0, struPlayBackListen.struIPAdress.szIP, 0, propertiesUtil.readValue("SmsBackServerListenIP").length());
        struPlayBackListen.struIPAdress.wPort = Short.parseShort(propertiesUtil.readValue("SmsBackServerListenPort")); //流媒体服务器监听端口
        struPlayBackListen.fnNewLinkCB = fPLAYBACK_NEWLINK_CB;
        struPlayBackListen.byLinkMode = 0; //0- TCP方式，1- UDP方式
        if (m_lPlayBackLinkHandle < 0) {
            m_lPlayBackListenHandle = hCEhomeStream.NET_ESTREAM_StartListenPlayBack(struPlayBackListen);
            if (m_lPlayBackListenHandle < -1) {
                System.out.println("NET_ESTREAM_StartListenPlayBack failed, error code:" + hCEhomeStream.NET_ESTREAM_GetLastError());
                hCEhomeStream.NET_ESTREAM_Fini();
                return;
            } else {
                String BackStreamListenInfo = new String(struPlayBackListen.struIPAdress.szIP).trim() + "_" + struPlayBackListen.struIPAdress.wPort;
                System.out.println("回放流媒体服务：" + BackStreamListenInfo + ",NET_ESTREAM_StartListenPlayBack succeed");
            }
        }
    }

    /**
     * 开启预览，
     *
     * @param lChannel 预览通道号
     */
    public void RealPlay(int lChannel) {
        HCISUPCMS.NET_EHOME_PREVIEWINFO_IN struPreviewIn = new HCISUPCMS.NET_EHOME_PREVIEWINFO_IN();
        struPreviewIn.iChannel = lChannel; //通道号
        struPreviewIn.dwLinkMode = 0; //0- TCP方式，1- UDP方式
        struPreviewIn.dwStreamType = 0; //码流类型：0- 主码流，1- 子码流, 2- 第三码流
        struPreviewIn.struStreamSever.szIP = propertiesUtil.readValue("SmsServerIP").getBytes();//流媒体服务器IP地址,公网地址
        struPreviewIn.struStreamSever.wPort = Short.parseShort(propertiesUtil.readValue("SmsServerPort")); //流媒体服务器端口，需要跟服务器启动监听端口一致
        struPreviewIn.write();
        //预览请求
        HCISUPCMS.NET_EHOME_PREVIEWINFO_OUT struPreviewOut = new HCISUPCMS.NET_EHOME_PREVIEWINFO_OUT();
        boolean getRS = CmsDemo.hCEhomeCMS.NET_ECMS_StartGetRealStream(CmsDemo.lLoginID, struPreviewIn, struPreviewOut);
        //Thread.sleep(10000);
        if (!CmsDemo.hCEhomeCMS.NET_ECMS_StartGetRealStream(CmsDemo.lLoginID, struPreviewIn, struPreviewOut)) {
            System.out.println("NET_ECMS_StartGetRealStream failed, error code:" + CmsDemo.hCEhomeCMS.NET_ECMS_GetLastError());
            return;
        } else {
            struPreviewOut.read();
            System.out.println("NET_ECMS_StartGetRealStream succeed, sessionID:" + struPreviewOut.lSessionID);
            sessionID = struPreviewOut.lSessionID;
        }
        HCISUPCMS.NET_EHOME_PUSHSTREAM_IN struPushInfoIn = new HCISUPCMS.NET_EHOME_PUSHSTREAM_IN();
        struPushInfoIn.read();
        struPushInfoIn.dwSize = struPushInfoIn.size();
        struPushInfoIn.lSessionID = sessionID;
        struPushInfoIn.write();
        HCISUPCMS.NET_EHOME_PUSHSTREAM_OUT struPushInfoOut = new HCISUPCMS.NET_EHOME_PUSHSTREAM_OUT();
        struPushInfoOut.read();
        struPushInfoOut.dwSize = struPushInfoOut.size();
        struPushInfoOut.write();
        if (!CmsDemo.hCEhomeCMS.NET_ECMS_StartPushRealStream(CmsDemo.lLoginID, struPushInfoIn, struPushInfoOut)) {
            System.out.println("NET_ECMS_StartPushRealStream failed, error code:" + CmsDemo.hCEhomeCMS.NET_ECMS_GetLastError());
            return;
        } else {
            System.out.println("NET_ECMS_StartPushRealStream succeed, sessionID:" + struPushInfoIn.lSessionID);
        }
    }

    /**
     * 停止预览,Stream服务停止实时流转发，CMS向设备发送停止预览请求
     */
    public void StopRealPlay() {
        if (!hCEhomeStream.NET_ESTREAM_StopPreview(lPreviewHandle)) {
            System.out.println("NET_ESTREAM_StopPreview failed,err = " + hCEhomeStream.NET_ESTREAM_GetLastError());
            return;
        }
        System.out.println("停止Stream的实时流转发");
        if (!CmsDemo.hCEhomeCMS.NET_ECMS_StopGetRealStream(CmsDemo.lLoginID, sessionID)) {
            System.out.println("NET_ECMS_StopGetRealStream failed,err = " + CmsDemo.hCEhomeCMS.NET_ECMS_GetLastError());
            return;
        }
        System.out.println("CMS发送预览停止请求");
    }


    /**
     * 按时间回放，ISUP5.0接入支持，需要确认设备上对应时间段是否有录像存储，设备中没有录像文件无法集成此模块
     *
     * @param lchannel 通道号
     */
    public void PlayBackByTime(int lchannel) {

        HCISUPCMS.NET_EHOME_PLAYBACK_INFO_IN m_struPlayBackInfoIn = new HCISUPCMS.NET_EHOME_PLAYBACK_INFO_IN();
        m_struPlayBackInfoIn.read();
        m_struPlayBackInfoIn.dwSize = m_struPlayBackInfoIn.size();
        m_struPlayBackInfoIn.dwChannel = lchannel; //通道号
        m_struPlayBackInfoIn.byPlayBackMode = 1;//0- 按文件名回放，1- 按时间回放
        m_struPlayBackInfoIn.unionPlayBackMode.setType(HCISUPCMS.NET_EHOME_PLAYBACKBYTIME.class);
        m_struPlayBackInfoIn.unionPlayBackMode.struPlayBackbyTime.struStartTime.wYear = 2022;
        m_struPlayBackInfoIn.unionPlayBackMode.struPlayBackbyTime.struStartTime.byMonth = 7;
        m_struPlayBackInfoIn.unionPlayBackMode.struPlayBackbyTime.struStartTime.byDay = 25;
        m_struPlayBackInfoIn.unionPlayBackMode.struPlayBackbyTime.struStartTime.byHour = 8;
        m_struPlayBackInfoIn.unionPlayBackMode.struPlayBackbyTime.struStartTime.byMinute = 0;
        m_struPlayBackInfoIn.unionPlayBackMode.struPlayBackbyTime.struStartTime.bySecond = 0;

        m_struPlayBackInfoIn.unionPlayBackMode.struPlayBackbyTime.struStopTime.wYear = 2022;
        m_struPlayBackInfoIn.unionPlayBackMode.struPlayBackbyTime.struStopTime.byMonth = 7;
        m_struPlayBackInfoIn.unionPlayBackMode.struPlayBackbyTime.struStopTime.byDay = 25;
        m_struPlayBackInfoIn.unionPlayBackMode.struPlayBackbyTime.struStopTime.byHour = 8;
        m_struPlayBackInfoIn.unionPlayBackMode.struPlayBackbyTime.struStopTime.byMinute = 20;
        m_struPlayBackInfoIn.unionPlayBackMode.struPlayBackbyTime.struStopTime.bySecond = 0;

        System.arraycopy(propertiesUtil.readValue("SmsBackServerIP").getBytes(), 0, m_struPlayBackInfoIn.struStreamSever.szIP,
                0, propertiesUtil.readValue("SmsBackServerIP").length());
        m_struPlayBackInfoIn.struStreamSever.wPort = Short.parseShort(propertiesUtil.readValue("SmsBackServerPort"));
        m_struPlayBackInfoIn.write();
        HCISUPCMS.NET_EHOME_PLAYBACK_INFO_OUT m_struPlayBackInfoOut = new HCISUPCMS.NET_EHOME_PLAYBACK_INFO_OUT();
        m_struPlayBackInfoOut.write();
        if (!CmsDemo.hCEhomeCMS.NET_ECMS_StartPlayBack(CmsDemo.lLoginID, m_struPlayBackInfoIn, m_struPlayBackInfoOut)) {
            System.out.println("NET_ECMS_StartPlayBack failed, error code:" + CmsDemo.hCEhomeCMS.NET_ECMS_GetLastError());
            return;
        } else {
            m_struPlayBackInfoOut.read();
            System.out.println("NET_ECMS_StartPlayBack succeed, lSessionID:" + m_struPlayBackInfoOut.lSessionID);
        }

        HCISUPCMS.NET_EHOME_PUSHPLAYBACK_IN m_struPushPlayBackIn = new HCISUPCMS.NET_EHOME_PUSHPLAYBACK_IN();
        m_struPushPlayBackIn.read();
        m_struPushPlayBackIn.dwSize = m_struPushPlayBackIn.size();
        m_struPushPlayBackIn.lSessionID = m_struPlayBackInfoOut.lSessionID;
        m_struPushPlayBackIn.write();

        backSessionID = m_struPushPlayBackIn.lSessionID;

        HCISUPCMS.NET_EHOME_PUSHPLAYBACK_OUT m_struPushPlayBackOut = new HCISUPCMS.NET_EHOME_PUSHPLAYBACK_OUT();
        m_struPushPlayBackOut.read();
        m_struPushPlayBackOut.dwSize = m_struPushPlayBackOut.size();
        m_struPushPlayBackOut.write();

        if (!CmsDemo.hCEhomeCMS.NET_ECMS_StartPushPlayBack(CmsDemo.lLoginID, m_struPushPlayBackIn, m_struPushPlayBackOut)) {
            System.out.println("NET_ECMS_StartPushPlayBack failed, error code:" + CmsDemo.hCEhomeCMS.NET_ECMS_GetLastError());
            return;
        } else {
            System.out.println("NET_ECMS_StartPushPlayBack succeed, sessionID:" + m_struPushPlayBackIn.lSessionID + ",lUserID:" + CmsDemo.lLoginID);
        }
    }


    /**
     * 停止回放
     */
    public void stopPlayBackByTime() {
        if (!CmsDemo.hCEhomeCMS.NET_ECMS_StopPlayBack(CmsDemo.lLoginID, backSessionID)) {
            System.out.println("NET_ECMS_StopPlayBack failed,err = " + CmsDemo.hCEhomeCMS.NET_ECMS_GetLastError());
            return;
        }
        System.out.println("CMS发送回放停止请求");
        if (!hCEhomeStream.NET_ESTREAM_StopPlayBack(m_lPlayBackLinkHandle)) {
            System.out.println("NET_ESTREAM_StopPlayBack failed,err = " + hCEhomeStream.NET_ESTREAM_GetLastError());
            return;
        }
        System.out.println("停止回放Stream服务的实时流转发");
    }

    //测试暂停
    public void testPause() {
        HCISUPCMS.NET_EHOME_PLAYBACK_PAUSE_RESTART_PARAM struPlaybackPauseParam = new HCISUPCMS.NET_EHOME_PLAYBACK_PAUSE_RESTART_PARAM();
        struPlaybackPauseParam.read();
        struPlaybackPauseParam.lSessionID = backSessionID;
        struPlaybackPauseParam.write();
        if (!CmsDemo.hCEhomeCMS.NET_ECMS_PlayBackOperate(CmsDemo.lLoginID, 0, struPlaybackPauseParam.getPointer())) {
            System.out.println("NET_ECMS_PlayBackOperate failed, error code:" + CmsDemo.hCEhomeCMS.NET_ECMS_GetLastError());
            return;
        } else {
            System.out.println("NET_ECMS_PlayBackOperate succeed, sessionID:" + backSessionID);
        }
    }

    /**
     * 播放库加载
     *
     * @return
     */
    private static boolean CreatePlayInstance() {
        if (playCtrl == null) {
            synchronized (PlayCtrl.class) {
                String strPlayPath = "";
                try {
                    if (osSelect.isWindows())
                        //win系统加载库路径(路径不要带中文)
                        strPlayPath = System.getProperty("user.dir") + "\\lib\\PlayCtrl.dll";
                    else if (osSelect.isLinux())
                        //Linux系统加载库路径(路径不要带中文)
                        strPlayPath = System.getProperty("user.dir") + "/lib/libPlayCtrl.so";
                    playCtrl = (PlayCtrl) Native.loadLibrary(strPlayPath, PlayCtrl.class);

                } catch (Exception ex) {
                    System.out.println("loadLibrary: " + strPlayPath + " Error: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }


    public class FPREVIEW_NEWLINK_CB implements HCISUPStream.PREVIEW_NEWLINK_CB {
        public boolean invoke(int lLinkHandle, HCISUPStream.NET_EHOME_NEWLINK_CB_MSG pNewLinkCBMsg, Pointer pUserData) {
//            System.out.println("FPREVIEW_NEWLINK_CB callback");

            //预览数据回调参数
            lPreviewHandle = lLinkHandle;
            HCISUPStream.NET_EHOME_PREVIEW_DATA_CB_PARAM struDataCB = new HCISUPStream.NET_EHOME_PREVIEW_DATA_CB_PARAM();
            if (fPREVIEW_DATA_CB == null) {
                fPREVIEW_DATA_CB = new FPREVIEW_DATA_CB();
            }
            struDataCB.fnPreviewDataCB = fPREVIEW_DATA_CB;

            if (!hCEhomeStream.NET_ESTREAM_SetPreviewDataCB(lLinkHandle, struDataCB)) {
                System.out.println("NET_ESTREAM_SetPreviewDataCB failed err:：" + hCEhomeStream.NET_ESTREAM_GetLastError());
                return false;
            }
            return true;
        }
    }

    public class FPREVIEW_DATA_CB implements HCISUPStream.PREVIEW_DATA_CB {
        //实时流回调函数/
        public void invoke(int iPreviewHandle, HCISUPStream.NET_EHOME_PREVIEW_CB_MSG pPreviewCBMsg, Pointer pUserData) {
            long offset = 0;
            ByteBuffer buffers = pPreviewCBMsg.pRecvdata.getByteBuffer(offset, pPreviewCBMsg.dwDataLen);
            byte[] bytes = new byte[pPreviewCBMsg.dwDataLen];
            buffers.rewind();
            buffers.get(bytes);

            try {
                H264UtilV1.writeESH264(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public static class PLAYBACK_NEWLINK_CB implements HCISUPStream.PLAYBACK_NEWLINK_CB {
        public boolean invoke(int lPlayBackLinkHandle, HCISUPStream.NET_EHOME_PLAYBACK_NEWLINK_CB_INFO pNewLinkCBInfo, Pointer pUserData) {
            pNewLinkCBInfo.read();
            System.out.println("PLAYBACK_NEWLINK_CB callback, szDeviceID:" + new String(pNewLinkCBInfo.szDeviceID).trim()
                    + ",lSessionID:" + pNewLinkCBInfo.lSessionID + ",dwChannelNo:" + pNewLinkCBInfo.dwChannelNo);
            m_lPlayBackLinkHandle = lPlayBackLinkHandle;
            HCISUPStream.NET_EHOME_PLAYBACK_DATA_CB_PARAM struCBParam = new HCISUPStream.NET_EHOME_PLAYBACK_DATA_CB_PARAM();
            //预览数据回调参数
            if (fPLAYBACK_DATA_CB == null) {
                fPLAYBACK_DATA_CB = new PLAYBACK_DATA_CB();
            }
//            pNewLinkCBInfo.fnPlayBackDataCB = fPLAYBACK_DATA_CB;
//            pNewLinkCBInfo.byStreamFormat = 0;
            struCBParam.fnPlayBackDataCB = fPLAYBACK_DATA_CB;
            struCBParam.byStreamFormat = 0;
            struCBParam.write();
//            pNewLinkCBInfo.write();
            if (!hCEhomeStream.NET_ESTREAM_SetPlayBackDataCB(lPlayBackLinkHandle, struCBParam)) {
                System.out.println("NET_ESTREAM_SetPlayBackDataCB failed");
            }
            return true;
        }
    }

    public static class PLAYBACK_DATA_CB implements HCISUPStream.PLAYBACK_DATA_CB {
        //实时流回调函数
        public boolean invoke(int iPlayBackLinkHandle, HCISUPStream.NET_EHOME_PLAYBACK_DATA_CB_INFO pDataCBInfo, Pointer pUserData) {
            //如果仅需保存回放的视频流文件，参考一下注释的代码
            try {
                m_file = new FileOutputStream(file, true);
                long offset = 0;
                ByteBuffer buffers = pDataCBInfo.pData.getByteBuffer(offset, pDataCBInfo.dwDataLen);
                byte[] bytes = new byte[pDataCBInfo.dwDataLen];
                buffers.rewind();
                buffers.get(bytes);
                m_file.write(bytes);
                m_file.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return true;
        }
    }
}



