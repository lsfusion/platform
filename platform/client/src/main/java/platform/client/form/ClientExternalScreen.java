package platform.client.form;

import platform.client.Main;
import platform.interop.form.layout.SimplexConstraints;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenComponent;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientExternalScreen {

    private static List<ClientExternalScreen> screens = new ArrayList<ClientExternalScreen>();
    public static ClientExternalScreen getScreen(int screenID) {

        for (ClientExternalScreen screen : screens)
            if (screen.getID() == screenID)
                return screen;

        ClientExternalScreen screen = null;
        try {
            screen = new ClientExternalScreen(Main.remoteLogics.getExternalScreen(screenID));
        } catch (RemoteException e) {
            throw new RuntimeException("Ошибка при считывании параметров внешнего экрана", e);
        }
        screen.initialize();
        screens.add(screen);
        return screen;
    }

    public int getID() {
        return screen.getID();
    }

    public void initialize() {
        screen.initialize();
    }

    ExternalScreen screen;

    public ClientExternalScreen(ExternalScreen screen) {
        this.screen = screen;
    }

    private transient Map<Integer, Map<ExternalScreenComponent, SimplexConstraints>> components = new HashMap<Integer, Map<ExternalScreenComponent, SimplexConstraints>>();
    public void add(int formID, ExternalScreenComponent comp, SimplexConstraints cons) {
        if (!components.containsKey(formID)) {
            components.put(formID, new HashMap<ExternalScreenComponent, SimplexConstraints>());
        }
        components.get(formID).put(comp, cons);
    }

    public void repaint(int formID) {
        screen.repaint(components.get(formID));
    }
}
