package platform.client.form;

import platform.client.ClientResourceBundle;
import platform.client.Main;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenComponent;
import platform.interop.form.screen.ExternalScreenConstraints;
import platform.interop.form.screen.ExternalScreenParameters;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class ClientExternalScreen {

    private static Map<Integer, ClientExternalScreen> screens = new HashMap<Integer, ClientExternalScreen>();
    public static ClientExternalScreen getScreen(int screenID) {
        ClientExternalScreen screen = screens.get(screenID);
        if (screen == null) {
            try {
                screen = new ClientExternalScreen(Main.remoteLogics.getExternalScreen(screenID));
                screen.initialize();
                screens.put(screenID, screen);
            } catch (RemoteException e) {
                throw new RuntimeException(ClientResourceBundle.getString("errors.error.reading.parameters.of.the.external.screen"), e);
            }
        }

        return screen;
    }

    public static void dropCaches() {
        screens.clear();
    }

    private boolean valid = true;

    public int getID() {
        return screen.getID();
    }

    public void initialize() {
        try {
            ExternalScreenParameters params = Main.remoteLogics.getExternalScreenParameters(getID(), Main.computerId);
            screen.initialize(params);
        } catch (RemoteException e) {
            throw new RuntimeException(ClientResourceBundle.getString("errors.error.reading.parameters.of.the.external.screen"), e);
        }
    }

    ExternalScreen screen;

    public ClientExternalScreen(ExternalScreen screen) {
        this.screen = screen;
    }

    private transient Map<Integer, Map<ExternalScreenComponent, ExternalScreenConstraints>> components = new HashMap<Integer, Map<ExternalScreenComponent, ExternalScreenConstraints>>();

    public void add(int formID, ExternalScreenComponent comp, ExternalScreenConstraints cons) {
        if (!components.containsKey(formID)) {
            components.put(formID, new HashMap<ExternalScreenComponent, ExternalScreenConstraints>());
        }
        components.get(formID).put(comp, cons);
    }

    public void remove(int formID, ExternalScreenComponent comp) {
        components.get(formID).remove(comp);
    }

    public void invalidate() {
        valid = false;
    }

    public static void invalidate(int formID) {
        for (ClientExternalScreen curScreen : screens.values()) {
            if (curScreen.components.containsKey(formID) && !curScreen.components.get(formID).isEmpty()) {
                curScreen.invalidate();
            }
        }
    }

    public static void repaintAll(int formID) {
        for (ClientExternalScreen curScreen : screens.values()) {
            if (!curScreen.valid) {
                if (curScreen.components.containsKey(formID) && !curScreen.components.get(formID).isEmpty()) {
                    curScreen.screen.repaint(curScreen.components.get(formID));
                    curScreen.valid = true;
                }
            }
        }
    }
}
