package platform.client.form;

import platform.client.Main;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenComponent;
import platform.interop.form.screen.ExternalScreenConstraints;
import platform.interop.form.screen.ExternalScreenParameters;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientExternalScreen {

    private static Map<Integer, ClientExternalScreen> screens = new HashMap<Integer, ClientExternalScreen>();
    private boolean valid = true;

    public static ClientExternalScreen getScreen(int screenID) {
        ClientExternalScreen screen = screens.get(screenID);
        if (screen == null) {
            try {
                // нужно оставить этот вызов здесь, чтобы класс параметров экрана подгрузился до создания самого объекта экрана
                ExternalScreenParameters params = Main.remoteLogics.getExternalScreenParameters(screenID, Main.computerId);

                screen = new ClientExternalScreen(Main.remoteLogics.getExternalScreen(screenID));
                screen.initialize();
                screens.put(screenID, screen);
            } catch (RemoteException e) {
                throw new RuntimeException("Ошибка при считывании параметров внешнего экрана", e);
            }
        }

        return screen;
    }

    public int getID() {
        return screen.getID();
    }

    public void initialize() {
        try {
            ExternalScreenParameters params = Main.remoteLogics.getExternalScreenParameters(getID(), Main.computerId);
            screen.initialize(params);
        } catch (RemoteException e) {
            throw new RuntimeException("Ошибка при считывании параметров внешнего экрана", e);
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
