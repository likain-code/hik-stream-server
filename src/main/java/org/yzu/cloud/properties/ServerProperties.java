package org.yzu.cloud.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stream.server")
public class ServerProperties {

    private String cmsServerIP;
    private String cmsServerPort;
    private String smsServerIP;
    private String smsServerPort;
    private String smsServerListenIP;
    private String smsServerListenPort;

    public String getCmsServerIP() {
        return cmsServerIP;
    }

    public void setCmsServerIP(String cmsServerIP) {
        this.cmsServerIP = cmsServerIP;
    }

    public String getCmsServerPort() {
        return cmsServerPort;
    }

    public void setCmsServerPort(String cmsServerPort) {
        this.cmsServerPort = cmsServerPort;
    }

    public String getSmsServerIP() {
        return smsServerIP;
    }

    public void setSmsServerIP(String smsServerIP) {
        this.smsServerIP = smsServerIP;
    }

    public String getSmsServerPort() {
        return smsServerPort;
    }

    public void setSmsServerPort(String smsServerPort) {
        this.smsServerPort = smsServerPort;
    }

    public String getSmsServerListenIP() {
        return smsServerListenIP;
    }

    public void setSmsServerListenIP(String smsServerListenIP) {
        this.smsServerListenIP = smsServerListenIP;
    }

    public String getSmsServerListenPort() {
        return smsServerListenPort;
    }

    public void setSmsServerListenPort(String smsServerListenPort) {
        this.smsServerListenPort = smsServerListenPort;
    }
}
