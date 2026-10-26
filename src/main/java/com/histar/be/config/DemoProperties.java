package com.histar.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo")
public class DemoProperties {

    private boolean enabled = false;
    private String secret = "";
    private String adminBridgeSecret = "";
    private String frontendUrl = "";
    private String adminEmail = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getAdminBridgeSecret() { return adminBridgeSecret; }
    public void setAdminBridgeSecret(String value) { this.adminBridgeSecret = value; }
    public String getFrontendUrl() { return frontendUrl; }
    public void setFrontendUrl(String value) { this.frontendUrl = value; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String value) { this.adminEmail = value; }
}
