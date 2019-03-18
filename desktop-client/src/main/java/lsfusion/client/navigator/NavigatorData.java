package lsfusion.client.navigator;

import lsfusion.client.navigator.window.ClientAbstractWindow;
import lsfusion.client.navigator.window.ClientNavigatorWindow;

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

    public final ClientAbstractWindow logs;
    public final ClientAbstractWindow status;
    public final ClientAbstractWindow forms;

    public NavigatorData(ClientNavigatorElement root, Map<String, ClientNavigatorWindow> windows,
                         ClientAbstractWindow logs, ClientAbstractWindow status, ClientAbstractWindow forms) {
        this.root = root;
        this.windows = windows;
        this.logs = logs;
        this.status = status;
        this.forms = forms;
    }

    public static NavigatorData deserializeListClientNavigatorElementWithChildren(byte[] state) throws IOException {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(state));

        Map<String, ClientNavigatorWindow> windows = new HashMap<>();
        List<ClientNavigatorElement> elements = new ArrayList<>();
        int elementsCount = inStream.readInt();
        for (int i1 = 0; i1 < elementsCount; i1++) {
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
                child.parents.add(element);
            }
        }

        ClientAbstractWindow logs =new ClientAbstractWindow(inStream);
        ClientAbstractWindow status = new ClientAbstractWindow(inStream);
        ClientAbstractWindow forms = new ClientAbstractWindow(inStream);

        return new NavigatorData(elements.isEmpty() ? null : elements.get(0), windows, logs, status, forms);
    }
}
