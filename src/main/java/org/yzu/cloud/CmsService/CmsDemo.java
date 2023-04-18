package org.yzu.cloud.CmsService;

import org.yzu.cloud.Common.PropertiesUtil;
import org.yzu.cloud.Common.osSelect;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.io.*;

public class CmsDemo {

    public static int lLoginID = -1;
    public static HCISUPCMS hCEhomeCMS = null;
    public static int CmsHandle = -1; //CMS监听句柄
    static FRegisterCallBack fRegisterCallBack;//注册回调函数实现
    HCISUPCMS.NET_EHOME_CMS_LISTEN_PARAM struCMSListenPara = new HCISUPCMS.NET_EHOME_CMS_LISTEN_PARAM();
    static String configPath = "/home/config.properties";

    PropertiesUtil propertiesUtil;
    {
        try {
            propertiesUtil = new PropertiesUtil(configPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据不同操作系统选择不同的库文件和库路径
     *
     * @return
     */
    private static boolean CreateSDKInstance() {
        if (hCEhomeCMS == null) {
            synchronized (HCISUPCMS.class) {
                String strDllPath = "";
                try {
                    //System.setProperty("jna.debug_load", "true");
                    if (osSelect.isWindows())
                        //win系统加载库路径(路径不要带中文)
                        strDllPath = System.getProperty("user.dir") + "\\lib\\HCISUPCMS.dll";
                    else if (osSelect.isLinux())
                        //Linux系统加载库路径(路径不要带中文)
                        strDllPath = System.getProperty("user.dir") + "/lib/libHCISUPCMS.so";
                    hCEhomeCMS = (HCISUPCMS) Native.loadLibrary(strDllPath, HCISUPCMS.class);
                } catch (Exception ex) {
                    System.out.println("loadLibrary: " + strDllPath + " Error: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * cms服务初始化，开启监听
     *
     * @throws IOException
     */
    public void cMS_Init() throws IOException {

        if (hCEhomeCMS == null) {
            if (!CreateSDKInstance()) {
                System.out.println("Load CMS SDK fail");
                return;
            }
        }
        if (osSelect.isWindows()) {
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCrypto = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCrypto = System.getProperty("user.dir") + "\\lib\\libeay32.dll"; //Linux版本是libcrypto.so库文件的路径
            System.arraycopy(strPathCrypto.getBytes(), 0, ptrByteArrayCrypto.byValue, 0, strPathCrypto.length());
            ptrByteArrayCrypto.write();
            hCEhomeCMS.NET_ECMS_SetSDKInitCfg(0, ptrByteArrayCrypto.getPointer());

            //设置libssl.so所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArraySsl = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathSsl = System.getProperty("user.dir") + "\\lib\\ssleay32.dll";    //Linux版本是libssl.so库文件的路径
            System.arraycopy(strPathSsl.getBytes(), 0, ptrByteArraySsl.byValue, 0, strPathSsl.length());
            ptrByteArraySsl.write();
            hCEhomeCMS.NET_ECMS_SetSDKInitCfg(1, ptrByteArraySsl.getPointer());
            //注册服务初始化
            boolean binit = hCEhomeCMS.NET_ECMS_Init();
            //设置HCAapSDKCom组件库文件夹所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCom = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCom = System.getProperty("user.dir") + "\\lib\\HCAapSDKCom";        //只支持绝对路径，建议使用英文路径
            System.arraycopy(strPathCom.getBytes(), 0, ptrByteArrayCom.byValue, 0, strPathCom.length());
            ptrByteArrayCom.write();
            hCEhomeCMS.NET_ECMS_SetSDKLocalCfg(5, ptrByteArrayCom.getPointer());

        } else if (osSelect.isLinux()) {
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCrypto = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCrypto = System.getProperty("user.dir") + "/lib/libcrypto.so"; //Linux版本是libcrypto.so库文件的路径
            System.arraycopy(strPathCrypto.getBytes(), 0, ptrByteArrayCrypto.byValue, 0, strPathCrypto.length());
            ptrByteArrayCrypto.write();
            hCEhomeCMS.NET_ECMS_SetSDKInitCfg(0, ptrByteArrayCrypto.getPointer());

            //设置libssl.so所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArraySsl = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathSsl = System.getProperty("user.dir") + "/lib/libssl.so";    //Linux版本是libssl.so库文件的路径
            System.arraycopy(strPathSsl.getBytes(), 0, ptrByteArraySsl.byValue, 0, strPathSsl.length());
            ptrByteArraySsl.write();
            hCEhomeCMS.NET_ECMS_SetSDKInitCfg(1, ptrByteArraySsl.getPointer());
            //注册服务初始化
            boolean binit = hCEhomeCMS.NET_ECMS_Init();
            //设置HCAapSDKCom组件库文件夹所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCom = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCom = System.getProperty("user.dir") + "/lib/HCAapSDKCom/";        //只支持绝对路径，建议使用英文路径
            System.arraycopy(strPathCom.getBytes(), 0, ptrByteArrayCom.byValue, 0, strPathCom.length());
            ptrByteArrayCom.write();
            hCEhomeCMS.NET_ECMS_SetSDKLocalCfg(5, ptrByteArrayCom.getPointer());

        }
        hCEhomeCMS.NET_ECMS_SetLogToFile(3, System.getProperty("user.dir") + "/EHomeSDKLog", false);
    }

    public void startCmsListen() {
        if (fRegisterCallBack == null) {
            fRegisterCallBack = new FRegisterCallBack();
        }
        System.arraycopy(propertiesUtil.readValue("CmsServerIP").getBytes(), 0, struCMSListenPara.struAddress.szIP, 0, propertiesUtil.readValue("CmsServerIP").length());
        struCMSListenPara.struAddress.wPort = Short.parseShort(propertiesUtil.readValue("CmsServerPort"));
        struCMSListenPara.fnCB = fRegisterCallBack;
        struCMSListenPara.write();
        //启动监听，接收设备注册信息
        CmsHandle = hCEhomeCMS.NET_ECMS_StartListen(struCMSListenPara);
        if (CmsHandle < -1) {
            System.out.println("NET_ECMS_StartListen failed, error code:" + hCEhomeCMS.NET_ECMS_GetLastError());
            hCEhomeCMS.NET_ECMS_Fini();
            return;
        }
        String CmsListenInfo = new String(struCMSListenPara.struAddress.szIP).trim() + "_" + struCMSListenPara.struAddress.wPort;
        System.out.println("注册服务器:" + CmsListenInfo + ",NET_ECMS_StartListen succeed!\n");
    }

    //注册回调函数
    public class FRegisterCallBack implements HCISUPCMS.DEVICE_REGISTER_CB {
        public boolean invoke(int lUserID, int dwDataType, Pointer pOutBuffer, int dwOutLen, Pointer pInBuffer, int dwInLen, Pointer pUser) {
            System.out.println("FRegisterCallBack, dwDataType:" + dwDataType + ", lUserID:" + lUserID);
            switch (dwDataType) {
                case 0:  //ENUM_DEV_ON
                    System.out.println("0");
                    HCISUPCMS.NET_EHOME_DEV_REG_INFO_V12 strDevRegInfo = new HCISUPCMS.NET_EHOME_DEV_REG_INFO_V12();
                    strDevRegInfo.write();
                    Pointer pDevRegInfo = strDevRegInfo.getPointer();
                    pDevRegInfo.write(0, pOutBuffer.getByteArray(0, strDevRegInfo.size()), 0, strDevRegInfo.size());
                    strDevRegInfo.read();
                    HCISUPCMS.NET_EHOME_SERVER_INFO_V50 strEhomeServerInfo = new HCISUPCMS.NET_EHOME_SERVER_INFO_V50();
                    strEhomeServerInfo.read();
                    //strEhomeServerInfo.dwSize = strEhomeServerInfo.size();
                    byte[] byCmsIP = new byte[0];
                    //设置报警服务器地址、端口、类型
                    byCmsIP = propertiesUtil.readValue("AlarmServerIP").getBytes();
                    System.arraycopy(byCmsIP, 0, strEhomeServerInfo.struUDPAlarmSever.szIP, 0, byCmsIP.length);
                    System.arraycopy(byCmsIP, 0, strEhomeServerInfo.struTCPAlarmSever.szIP, 0, byCmsIP.length);
                    //报警服务器类型：0- 只支持UDP协议上报，1- 支持UDP、TCP两种协议上报 2-MQTT

                    strEhomeServerInfo.dwAlarmServerType = Integer.parseInt(propertiesUtil.readValue("AlarmServerType"));
                    strEhomeServerInfo.struTCPAlarmSever.wPort = Short.parseShort(propertiesUtil.readValue("AlarmServerTCPPort"));
                    strEhomeServerInfo.struUDPAlarmSever.wPort = Short.parseShort(propertiesUtil.readValue("AlarmServerUDPPort"));

                    byte[] byClouldAccessKey = "test".getBytes();
                    System.arraycopy(byClouldAccessKey, 0, strEhomeServerInfo.byClouldAccessKey, 0, byClouldAccessKey.length);
                    byte[] byClouldSecretKey = "12345".getBytes();
                    System.arraycopy(byClouldSecretKey, 0, strEhomeServerInfo.byClouldSecretKey, 0, byClouldSecretKey.length);
                    strEhomeServerInfo.dwClouldPoolId = 1;

                    //设置图片存储服务器地址、端口、类型
                    byte[] bySSIP = new byte[0];
                    bySSIP = propertiesUtil.readValue("PicServerIP").getBytes();
                    System.arraycopy(bySSIP, 0, strEhomeServerInfo.struPictureSever.szIP, 0, bySSIP.length);
                    strEhomeServerInfo.struPictureSever.wPort = Short.parseShort(propertiesUtil.readValue("PicServerPort"));
                    strEhomeServerInfo.dwPicServerType = Integer.parseInt(propertiesUtil.readValue("PicServerType"));    //存储服务器（SS）类型：0-Tomcat，1-VRB，2-云存储，3-KMS，4-ISUP5.0。
                    strEhomeServerInfo.write();
                    dwInLen = strEhomeServerInfo.size();
                    pInBuffer.write(0, strEhomeServerInfo.getPointer().getByteArray(0, dwInLen), 0, dwInLen);

                    System.out.println("Device online, DeviceID is:" + new String(strDevRegInfo.struRegInfo.byDeviceID).trim());
                    CmsDemo.lLoginID = lUserID;
                    return true;
                case 3: //ENUM_DEV_AUTH
                    System.out.println("3");
                    strDevRegInfo = new HCISUPCMS.NET_EHOME_DEV_REG_INFO_V12();
                    strDevRegInfo.write();
                    pDevRegInfo = strDevRegInfo.getPointer();
                    pDevRegInfo.write(0, pOutBuffer.getByteArray(0, strDevRegInfo.size()), 0, strDevRegInfo.size());
                    strDevRegInfo.read();
                    byte[] bs = new byte[0];
                    String szEHomeKey = propertiesUtil.readValue("ISUPKey"); //ISUP5.0登录校验值
                    bs = szEHomeKey.getBytes();
                    pInBuffer.write(0, bs, 0, szEHomeKey.length());
                    break;
                case 5: //HCISUPCMS.ENUM_DEV_DAS_REQ
                    System.out.println("5");
                    String dasInfo = "{\n" +
                            "    \"Type\":\"DAS\",\n" +
                            "    \"DasInfo\":{\n" +
                            "        \"Address\":\"" + propertiesUtil.readValue("DasServerIP") + "\",\n" +
                            "        \"Domain\":\"\",\n" +
                            "        \"ServerID\":\"\",\n" +
                            "        \"Port\":" + propertiesUtil.readValue("DasServerPort") + ",\n" +
                            "        \"UdpPort\":\n" +
                            "    }\n" +
                            "}";
//                    System.out.println(dasInfo);
                    byte[] bs1 = dasInfo.getBytes();
                    pInBuffer.write(0, bs1, 0, dasInfo.length());
                    break;
                default:
                    System.out.println("FRegisterCallBack default type:" + dwDataType);
                    break;
            }
            return true;
        }
    }
}
