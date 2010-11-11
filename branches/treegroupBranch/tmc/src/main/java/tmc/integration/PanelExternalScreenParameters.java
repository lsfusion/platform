package tmc.integration;

import platform.interop.form.screen.ExternalScreenParameters;

public class PanelExternalScreenParameters implements ExternalScreenParameters {
    private final int screenComPort;
    private final int fiscalComPort;

    public PanelExternalScreenParameters(int screenComPort, int fiscalComPort) {
        this.screenComPort = screenComPort;
        this.fiscalComPort = fiscalComPort;
    }

    public int getScreenComPort() {
        return screenComPort;
    }

    public int getFiscalComPort() {
        return fiscalComPort;
    }
}