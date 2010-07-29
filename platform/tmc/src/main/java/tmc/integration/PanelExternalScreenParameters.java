package tmc.integration;

import platform.interop.form.screen.ExternalScreenParameters;

public class PanelExternalScreenParameters implements ExternalScreenParameters {
    private final int comPort;

    public PanelExternalScreenParameters(int comPort) {
        this.comPort = comPort;
    }

    public int getComPort() {
        return comPort;
    }
}