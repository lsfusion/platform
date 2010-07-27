package tmc.integration;

import platform.interop.form.screen.ExternalScreenParameters;

public class PanelExternalScreenParameters implements ExternalScreenParameters {
    private final String comPort;

    public PanelExternalScreenParameters(String comPort) {
        this.comPort = comPort;
    }

    public String getComPort() {
        return comPort;
    }
}