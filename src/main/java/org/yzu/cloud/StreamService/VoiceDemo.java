package org.yzu.cloud.StreamService;

import org.yzu.cloud.CmsService.CmsDemo;
import org.yzu.cloud.CmsService.HCISUPCMS;
import org.yzu.cloud.Common.PropertiesUtil;
import org.yzu.cloud.Common.osSelect;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.io.*;
import java.nio.ByteBuffer;

public class VoiceDemo {

    public class fVOICE_NEWLINK_CB implements HCISUPStream.VOICETALK_NEWLINK_CB {
        public boolean invoke(int lHandle, HCISUPStream.NET_EHOME_VOICETALK_NEWLINK_CB_INFO pNewLinkCBInfo, Pointer pUserData) {
            System.out.println("fVOICE_NEWLINK_CB callback");
            lVoiceLinkHandle = lHandle;
            HCISUPStream.NET_EHOME_VOICETALK_DATA_CB_PARAM net_ehome_voicetalk_data_cb_param = new HCISUPStream.NET_EHOME_VOICETALK_DATA_CB_PARAM();
            if (fVOICE_data_cb == null) {
                fVOICE_data_cb = new fVOICE_DATA_CB();
            }
            net_ehome_voicetalk_data_cb_param.fnVoiceTalkDataCB = fVOICE_data_cb;

            if (!hCEhomeVoice.NET_ESTREAM_SetVoiceTalkDataCB(lHandle, net_ehome_voicetalk_data_cb_param)) {
                System.out.println("NET_ESTREAM_SetVoiceTalkDataCB()错误代码号：" + hCEhomeVoice.NET_ESTREAM_GetLastError());
                return false;
            }
            return true;
        }
    }

    public class fVOICE_DATA_CB implements HCISUPStream.VOICETALK_DATA_CB {
        public boolean invoke(int lHandle, HCISUPStream.NET_EHOME_VOICETALK_DATA_CB_INFO pNewLinkCBInfo, Pointer pUserData) {
            //回调函数保存设备返回的语音数据
            //将设备发送过来的语音数据写入文件
            System.out.println("设备音频发送.....");
            VoiceHandle = lHandle;
            long offset = 0;
            ByteBuffer buffers = pNewLinkCBInfo.pData.getByteBuffer(offset, pNewLinkCBInfo.dwDataLen);
            byte[] bytes = new byte[pNewLinkCBInfo.dwDataLen];
            buffers.rewind();
            buffers.get(bytes);
            try {
                outputStreamPcm.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }

    }


    public static HCISUPStream hCEhomeVoice = null;
    static HCNetSDK hCNetSDK = null;
    public static int VoiceHandle = -1;   //语音监听句柄
    public static int lVoiceLinkHandle = -1; //语音连接句柄
    public static int VoicelServHandle = -1; //语音流媒体监听句柄
    static File fileEncode = null;
    static File filePcm = null;
    static FileOutputStream outputStreamG711 = null;
    static FileOutputStream outputStreamPcm = null;

    static fVOICE_NEWLINK_CB fVOICE_newlink_cb;//语音转发连接回调函数实现
    static fVOICE_DATA_CB fVOICE_data_cb; //语音数据回调函数


    HCISUPStream.NET_EHOME_LISTEN_VOICETALK_CFG net_ehome_listen_voicetalk_cfg = new HCISUPStream.NET_EHOME_LISTEN_VOICETALK_CFG();
    HCISUPCMS.NET_EHOME_VOICE_TALK_IN net_ehome_voice_talk_in = new HCISUPCMS.NET_EHOME_VOICE_TALK_IN();
    HCISUPCMS.NET_EHOME_VOICE_TALK_OUT net_ehome_voice_talk_out = new HCISUPCMS.NET_EHOME_VOICE_TALK_OUT();
    HCISUPCMS.NET_EHOME_PUSHVOICE_IN struPushVoiceIn = new HCISUPCMS.NET_EHOME_PUSHVOICE_IN();
    HCISUPCMS.NET_EHOME_PUSHVOICE_OUT struPushVoiceOut = new HCISUPCMS.NET_EHOME_PUSHVOICE_OUT();

    static String configPath = "./config.properties";
    PropertiesUtil propertiesUtil;

    {
        try {
            propertiesUtil = new PropertiesUtil(configPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void voice_Init() {
        if (hCEhomeVoice == null) {
            if (!CreateSDKInstance()) {
                System.out.println("Load Stream SDK fail");
                return;
            }

        }

        if (hCNetSDK == null) {
            if (!CreateNetSDKInstance()) {
                System.out.println("Load hcnetSDK fail");
                return;
            }
        }
        if (osSelect.isWindows()) {
            //设置libcrypto.so所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCrypto = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCrypto = System.getProperty("user.dir") + "\\lib\\libeay32.dll"; //Linux版本是libcrypto.so库文件的路径
            System.arraycopy(strPathCrypto.getBytes(), 0, ptrByteArrayCrypto.byValue, 0, strPathCrypto.length());
            ptrByteArrayCrypto.write();
            if (!hCEhomeVoice.NET_ESTREAM_SetSDKInitCfg(0, ptrByteArrayCrypto.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 0 failed, error:" + hCEhomeVoice.NET_ESTREAM_GetLastError());
            } else {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 0 succeed");
            }
            //设置libssl.so所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArraySsl = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathSsl = System.getProperty("user.dir") + "\\lib\\ssleay32.dll";    //Linux版本是libssl.so库文件的路径
            System.arraycopy(strPathSsl.getBytes(), 0, ptrByteArraySsl.byValue, 0, strPathSsl.length());
            ptrByteArraySsl.write();
            if (!hCEhomeVoice.NET_ESTREAM_SetSDKInitCfg(1, ptrByteArraySsl.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 1 failed, error:" + hCEhomeVoice.NET_ESTREAM_GetLastError());
            } else {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 1 succeed");
            }
            //语音流媒体初始化
            hCEhomeVoice.NET_ESTREAM_Init();
            //网络SDK初始化
            hCNetSDK.NET_DVR_Init();
            //设置HCAapSDKCom组件库文件夹所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCom = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCom = System.getProperty("user.dir") + "\\lib\\HCAapSDKCom";      //只支持绝对路径，建议使用英文路径
            System.arraycopy(strPathCom.getBytes(), 0, ptrByteArrayCom.byValue, 0, strPathCom.length());
            ptrByteArrayCom.write();
            if (!hCEhomeVoice.NET_ESTREAM_SetSDKLocalCfg(5, ptrByteArrayCom.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKLocalCfg 5 failed, error:" + hCEhomeVoice.NET_ESTREAM_GetLastError());
            } else {
                System.out.println("NET_ESTREAM_SetSDKLocalCfg 5 succeed");
            }
            hCEhomeVoice.NET_ESTREAM_SetLogToFile(3, "./EHomeSDKLog", false);
        } else if (osSelect.isLinux()) {
            //设置libcrypto.so所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCrypto = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCrypto = System.getProperty("user.dir") + "/lib/libcrypto.so"; //Linux版本是libcrypto.so库文件的路径
            System.arraycopy(strPathCrypto.getBytes(), 0, ptrByteArrayCrypto.byValue, 0, strPathCrypto.length());
            ptrByteArrayCrypto.write();
            if (!hCEhomeVoice.NET_ESTREAM_SetSDKInitCfg(0, ptrByteArrayCrypto.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 0 failed, error:" + hCEhomeVoice.NET_ESTREAM_GetLastError());
            } else {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 0 succeed");
            }
            //设置libssl.so所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArraySsl = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathSsl = System.getProperty("user.dir") + "/lib/libssl.so";    //Linux版本是libssl.so库文件的路径
            System.arraycopy(strPathSsl.getBytes(), 0, ptrByteArraySsl.byValue, 0, strPathSsl.length());
            ptrByteArraySsl.write();
            if (!hCEhomeVoice.NET_ESTREAM_SetSDKInitCfg(1, ptrByteArraySsl.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 1 failed, error:" + hCEhomeVoice.NET_ESTREAM_GetLastError());
            } else {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 1 succeed");
            }
            hCEhomeVoice.NET_ESTREAM_Init();

            //设置HCAapSDKCom组件库文件夹所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCom = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCom = System.getProperty("user.dir") + "/lib/HCAapSDKCom/";      //只支持绝对路径，建议使用英文路径
            System.arraycopy(strPathCom.getBytes(), 0, ptrByteArrayCom.byValue, 0, strPathCom.length());
            ptrByteArrayCom.write();
            if (!hCEhomeVoice.NET_ESTREAM_SetSDKLocalCfg(5, ptrByteArrayCom.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKLocalCfg 5 failed, error:" + hCEhomeVoice.NET_ESTREAM_GetLastError());
            } else {
                System.out.println("NET_ESTREAM_SetSDKLocalCfg 5 succeed");
            }
            hCNetSDK.NET_DVR_Init();

            hCEhomeVoice.NET_ESTREAM_SetLogToFile(3, "..\\EHomeSDKLog", false);
        }
    }

    /**
     * 开启语音流媒体服务监听
     */
    public void startVoiceServeListen()
    {
        if (fVOICE_newlink_cb == null) {
            fVOICE_newlink_cb = new fVOICE_NEWLINK_CB();
        }
        net_ehome_listen_voicetalk_cfg.struIPAdress.szIP = propertiesUtil.readValue("VoiceSmsServerListenIP").getBytes();
        net_ehome_listen_voicetalk_cfg.struIPAdress.wPort = Short.parseShort(propertiesUtil.readValue("VoiceSmsServerListenPort"));
        net_ehome_listen_voicetalk_cfg.fnNewLinkCB = fVOICE_newlink_cb;
        net_ehome_listen_voicetalk_cfg.byLinkMode = 0;
        net_ehome_listen_voicetalk_cfg.write();
        VoicelServHandle = hCEhomeVoice.NET_ESTREAM_StartListenVoiceTalk(net_ehome_listen_voicetalk_cfg);
        if (VoicelServHandle == -1) {
            System.out.println("NET_ESTREAM_StartListenPreview failed, error code:" + hCEhomeVoice.NET_ESTREAM_GetLastError());
            hCEhomeVoice.NET_ESTREAM_Fini();
            return;
        } else {
            String VoiceStreamListenInfo = new String(net_ehome_listen_voicetalk_cfg.struIPAdress.szIP).trim() + "_" + net_ehome_listen_voicetalk_cfg.struIPAdress.wPort;
            System.out.println("语音流媒体服务：" + VoiceStreamListenInfo + ",NET_ESTREAM_StartListenVoiceTalk succeed");
        }
    }

    /**
     * 开启语音转发
     */
    public void StartVoiceTrans() {

        net_ehome_voice_talk_in.struStreamSever.szIP = propertiesUtil.readValue("VoiceSmsServerIP").getBytes();
        net_ehome_voice_talk_in.struStreamSever.wPort = Short.parseShort(propertiesUtil.readValue("VoiceSmsServerPort"));
        net_ehome_voice_talk_in.dwVoiceChan = 1; //语音通道号
        net_ehome_voice_talk_in.write();
        //CMS 语音对讲请求
        if (!CmsDemo.hCEhomeCMS.NET_ECMS_StartVoiceWithStmServer(CmsDemo.lLoginID, net_ehome_voice_talk_in, net_ehome_voice_talk_out)) {
            System.out.println("NET_ECMS_StartVoiceWithStmServer failed, error code:" + CmsDemo.hCEhomeCMS.NET_ECMS_GetLastError());
            return;
        } else {
            net_ehome_voice_talk_out.read();
            System.out.println("NET_ECMS_StartVoiceWithStmServer suss sessionID=" + net_ehome_voice_talk_out.lSessionID);
        }

        filePcm = new File(System.getProperty("user.dir") + "\\AudioFile\\DevicetoPlat.g7");  //保存回调函数的音频数据

        if (!filePcm.exists()) {
            try {
                filePcm.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            outputStreamPcm = new FileOutputStream(filePcm);
        } catch (FileNotFoundException e3) {
            // TODO Auto-generated catch block
            e3.printStackTrace();
        }
        //CMS 请求开始推流
        struPushVoiceIn.dwSize = struPushVoiceIn.size();
        struPushVoiceIn.lSessionID = net_ehome_voice_talk_out.lSessionID;
        struPushVoiceOut.dwSize = struPushVoiceOut.size();
        if (!CmsDemo.hCEhomeCMS.NET_ECMS_StartPushVoiceStream(CmsDemo.lLoginID, struPushVoiceIn, struPushVoiceOut)) {
            System.out.println("NET_ECMS_StartPushVoiceStream failed, error code:" + CmsDemo.hCEhomeCMS.NET_ECMS_GetLastError());
            return;
        }
        System.out.println("NET_ECMS_StartPushVoiceStream!\n");

        //发送音频数据
        FileInputStream Voicefile = null;
        FileOutputStream Encodefile = null;
        int dataLength = 0;

        try {
            //创建从文件读取数据的FileInputStream流
            Voicefile = new FileInputStream(new File(System.getProperty("user.dir") + "\\AudioFile\\send2device.pcm"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        fileEncode = new File(System.getProperty("user.dir") + "\\AudioFile\\EncodeData.g7");  //保存音频编码数据

        if (!fileEncode.exists()) {
            try {
                fileEncode.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try {
            outputStreamG711 = new FileOutputStream(fileEncode);
        } catch (FileNotFoundException e3) {
            // TODO Auto-generated catch block
            e3.printStackTrace();
        }

        try {

            //返回文件的总字节数
            dataLength = Voicefile.available();
        } catch (IOException e1) {
            e1.printStackTrace();
        }


        if (dataLength < 0) {
            System.out.println("input file dataSize < 0");
//            return false;
        }

        HCNetSDK.BYTE_ARRAY ptrVoiceByte = new HCNetSDK.BYTE_ARRAY(dataLength);
        try {
            Voicefile.read(ptrVoiceByte.byValue);
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        ptrVoiceByte.write();

        int iEncodeSize = 0;
        HCNetSDK.NET_DVR_AUDIOENC_INFO enc_info = new HCNetSDK.NET_DVR_AUDIOENC_INFO();
        enc_info.write();
        Pointer encoder = hCNetSDK.NET_DVR_InitG711Encoder(enc_info);
        while ((dataLength - iEncodeSize) > 640) {
            HCNetSDK.BYTE_ARRAY ptrPcmData = new HCNetSDK.BYTE_ARRAY(640);
            System.arraycopy(ptrVoiceByte.byValue, iEncodeSize, ptrPcmData.byValue, 0, 640);
            ptrPcmData.write();

            HCNetSDK.BYTE_ARRAY ptrG711Data = new HCNetSDK.BYTE_ARRAY(320);
            ptrG711Data.write();

            HCNetSDK.NET_DVR_AUDIOENC_PROCESS_PARAM struEncParam = new HCNetSDK.NET_DVR_AUDIOENC_PROCESS_PARAM();
            struEncParam.in_buf = ptrPcmData.getPointer();
            struEncParam.out_buf = ptrG711Data.getPointer();
            struEncParam.out_frame_size = 320;
            struEncParam.g711_type = 0;//G711编码类型：0- U law，1- A law
            struEncParam.write();

            if (!hCNetSDK.NET_DVR_EncodeG711Frame(encoder, struEncParam)) {
                System.out.println("NET_DVR_EncodeG711Frame failed, error code:" + hCNetSDK.NET_DVR_GetLastError());
                hCNetSDK.NET_DVR_ReleaseG711Encoder(encoder);
                //	hCNetSDK.NET_DVR_StopVoiceCom(lVoiceHandle);
//                return false;
            }
            struEncParam.read();
            ptrG711Data.read();

            long offsetG711 = 0;
            ByteBuffer buffersG711 = struEncParam.out_buf.getByteBuffer(offsetG711, struEncParam.out_frame_size);
            byte[] bytesG711 = new byte[struEncParam.out_frame_size];
            buffersG711.rewind();
            buffersG711.get(bytesG711);
            try {
                outputStreamG711.write(bytesG711);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            iEncodeSize += 640;
            System.out.println("编码字节数：" + iEncodeSize);

            for (int i = 0; i < struEncParam.out_frame_size / 160; i++) {
                HCNetSDK.BYTE_ARRAY ptrG711Send = new HCNetSDK.BYTE_ARRAY(160);
                System.arraycopy(ptrG711Data.byValue, i * 160, ptrG711Send.byValue, 0, 160);
                ptrG711Send.write();
                HCISUPStream.NET_EHOME_VOICETALK_DATA struVoicTalkData = new HCISUPStream.NET_EHOME_VOICETALK_DATA();
                struVoicTalkData.pData = ptrG711Send.getPointer();
                struVoicTalkData.dwDataLen = 160;
                if (hCEhomeVoice.NET_ESTREAM_SendVoiceTalkData(lVoiceLinkHandle, struVoicTalkData) <= -1) {
                    System.out.println("NET_ESTREAM_SendVoiceTalkData failed, error code:" + hCEhomeVoice.NET_ESTREAM_GetLastError());
                }

                //需要实时速率发送数据
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }

    }


    public void StopVoiceTrans() {
        //SMS 停止语音对讲
        if (lVoiceLinkHandle >= 0) {
            if (!hCEhomeVoice.NET_ESTREAM_StopVoiceTalk(lVoiceLinkHandle)) {
                System.out.println("NET_ESTREAM_StopVoiceTalk failed, error code:" + hCEhomeVoice.NET_ESTREAM_GetLastError());
                return;
            }
        }
        //释放语音对讲请求资源
        if (!CmsDemo.hCEhomeCMS.NET_ECMS_StopVoiceTalkWithStmServer(CmsDemo.lLoginID, net_ehome_voice_talk_out.lSessionID)) {
            System.out.println("NET_ECMS_StopVoiceTalkWithStmServer failed, error code:" + CmsDemo.hCEhomeCMS.NET_ECMS_GetLastError());
            return;
        }
    }


    /**
     * 动态库加载
     *
     * @return
     */
    private static boolean CreateSDKInstance() {
        if (hCEhomeVoice == null) {
            synchronized (HCISUPStream.class) {
                String strDllPath = "";
                try {
                    if (osSelect.isWindows())
                        //win系统加载库路径(路径不要带中文)
                        strDllPath = System.getProperty("user.dir") + "\\lib\\HCISUPStream.dll";

                    else if (osSelect.isLinux())
                        //Linux系统加载库路径(路径不要带中文)
                        strDllPath = System.getProperty("user.dir")+"/lib/libHCISUPStream.so";
                    hCEhomeVoice = (HCISUPStream) Native.loadLibrary(strDllPath, HCISUPStream.class);
                } catch (Exception ex) {
                    System.out.println("loadLibrary: " + strDllPath + " Error: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;

    }

    /**
     * 根据不同操作系统选择不同的库文件和库路径
     *
     * @return
     */
    private static boolean CreateNetSDKInstance() {
        if (hCNetSDK == null) {
            synchronized (HCNetSDK.class) {
                String strDllPath = "";
                try {
                    //System.setProperty("jna.debug_load", "true");
                    if (osSelect.isWindows())
                        //win系统加载库路径
                        strDllPath = System.getProperty("user.dir") + "\\hcnetSDKlib\\HCNetSDK.dll";

                    else if (osSelect.isLinux())
                        //Linux系统加载库路径
                        strDllPath = System.getProperty("user.dir")+"/lib/libhcnetsdk.so";
                    hCNetSDK = (HCNetSDK) Native.loadLibrary(strDllPath, HCNetSDK.class);
                } catch (Exception ex) {
                    System.out.println("loadLibrary: " + strDllPath + " Error: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }


}






















