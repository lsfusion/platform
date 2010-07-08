package tmc.integration;

import platform.interop.form.layout.SimplexConstraints;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenComponent;

import java.util.Map;

public class PanelExternalScreen implements ExternalScreen {

    public int getID() {
        return 0;
    }

    public void initialize() {
        System.out.println("initialize");
    }

    public void repaint(Map<ExternalScreenComponent, SimplexConstraints> components) {
        System.out.println("repaint");
    }
}
