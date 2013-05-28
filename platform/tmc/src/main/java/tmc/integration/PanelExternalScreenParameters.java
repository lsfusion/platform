package tmc.integration;

import platform.interop.form.screen.ExternalScreenParameters;

public class PanelExternalScreenParameters implements ExternalScreenParameters {
    private final int screenComPort;
    private final int fiscalComPort;

    final boolean dispose;

    public PanelExternalScreenParameters(int screenComPort, int fiscalComPort) {
        this.screenComPort = screenComPort;
        this.fiscalComPort = fiscalComPort;

        String disposeProperty = System.getProperty("tmc.integration.exp.FiscalRegister.dispose");
        dispose = disposeProperty != null && disposeProperty.equals("true");
    }

    public int getScreenComPort() {
        return screenComPort;
    }

    public int getFiscalComPort() {
        return fiscalComPort;
    }
}