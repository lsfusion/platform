package lsfusion.client.navigator;

import lsfusion.client.navigator.window.ClientAbstractWindow;
import lsfusion.client.navigator.window.ClientNavigatorWindow;
import lsfusion.interop.navigator.NavigatorScheduler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavigatorData {
    public final ClientNavigatorElement root;
    public final Map<String, ClientNavigatorWindow> windows;

    public final ClientNavigatorChanges navigatorChanges;

    public final ClientAbstractWindow logs;
    public final ClientAbstractWindow forms;

    public final List<NavigatorScheduler> navigatorSchedulers;

    public NavigatorData(ClientNavigatorElement root, Map<String, ClientNavigatorWindow> windows,
                         ClientNavigatorChanges navigatorChanges,
                         ClientAbstractWindow logs, ClientAbstractWindow forms,
                         List<NavigatorScheduler> navigatorSchedulers) {
        this.root = root;
        this.windows = windows;
        this.navigatorChanges = navigatorChanges;
        this.logs = logs;
        this.forms = forms;
        this.navigatorSchedulers = navigatorSchedulers;
    }

    public static NavigatorData deserializeListClientNavigatorElementWithChildren(byte[] state) throws IOException {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(state));

        Map<String, ClientNavigatorWindow> windows = new HashMap<>();
        int windowsCount = inStream.readInt();
        for (int i = 0; i < windowsCount; i++) {
            ClientNavigatorWindow window = new ClientNavigatorWindow(inStream);
            windows.put(window.canonicalName, window);
        }

        List<ClientNavigatorElement> elements = new ArrayList<>();
        int elementsCount = inStream.readInt();
        for (int i = 0; i < elementsCount; i++) {
            elements.add(ClientNavigatorElement.deserialize(inStream, windows));
        }

        Map<String, ClientNavigatorElement> elementsMap = new HashMap<>();
        for (ClientNavigatorElement element : elements) {
            elementsMap.put(element.getCanonicalName(), element);
        }

        for (ClientNavigatorElement element : elements) {
            if (element.window != null) {
                element.window.elements.add(element);
            }
            int cnt = inStream.readInt();
            for (int i = 0; i < cnt; i++) {
                ClientNavigatorElement child = elementsMap.get(inStream.readUTF());

                element.children.add(child);
                child.parent = element;
            }
        }

        //deserialize and add to navigatordata
        ClientNavigatorChanges clientNavigatorChanges = new ClientNavigatorChanges(inStream);

        ClientAbstractWindow logs =new ClientAbstractWindow(inStream);
        ClientAbstractWindow forms = new ClientAbstractWindow(inStream);

        List<NavigatorScheduler> navigatorSchedulers = new ArrayList<>();
        int size = inStream.readInt();
        for (int i = 0; i < size; i++) {
            navigatorSchedulers.add(new NavigatorScheduler(inStream.readInt(), inStream.readBoolean()));
        }

        return new NavigatorData(elements.isEmpty() ? null : elements.get(0), windows, clientNavigatorChanges, logs, forms, navigatorSchedulers);
    }
}
