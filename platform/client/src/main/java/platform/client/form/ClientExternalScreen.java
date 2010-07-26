package platform.client.form;

import platform.client.Main;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenComponent;
import platform.interop.form.screen.ExternalScreenConstraints;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientExternalScreen {

    private static List<ClientExternalScreen> screens = new ArrayList<ClientExternalScreen>();
    private boolean valid = true;

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

    public static void repaintAll(int formID){
        for (ClientExternalScreen curScreen : screens){
            if (!curScreen.valid) {
                curScreen.screen.repaint(curScreen.components.get(formID));
                curScreen.valid = true;
            }
        }

    }

}
