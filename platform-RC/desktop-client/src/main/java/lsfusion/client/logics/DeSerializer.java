package lsfusion.client.logics;

import lsfusion.client.logics.classes.ClientClass;
import lsfusion.client.logics.classes.ClientTypeSerializer;
import lsfusion.client.navigator.ClientAbstractWindow;
import lsfusion.client.navigator.ClientNavigatorElement;
import lsfusion.client.navigator.ClientNavigatorWindow;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeSerializer {

    public static class NavigatorData {
        public final ClientNavigatorElement root;
        public final Map<String, ClientNavigatorWindow> windows;

        public NavigatorData(ClientNavigatorElement root,Map<String, ClientNavigatorWindow> windows) {
            this.root = root;
            this.windows = windows;
        }
    }

    public static NavigatorData deserializeListClientNavigatorElementWithChildren(byte[] state) throws IOException {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(state));

        Map<String, ClientNavigatorWindow> windows = new HashMap<>();
        List<ClientNavigatorElement> elements = deserializeListClientNavigatorElement(inStream, windows);

        Map<String, ClientNavigatorElement> elementsMap = new HashMap<>();
        for (ClientNavigatorElement element : elements) {
            elementsMap.put(element.getSID(), element);
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

        return new NavigatorData(elements.isEmpty() ? null : elements.get(0), windows);
    }

    public static List<ClientNavigatorElement> deserializeListClientNavigatorElement(byte[] state, Map<String, ClientNavigatorWindow> windows) throws IOException {
        return deserializeListClientNavigatorElement(new DataInputStream(new ByteArrayInputStream(state)), windows);
    }

    private static List<ClientNavigatorElement> deserializeListClientNavigatorElement(DataInputStream dataStream, Map<String, ClientNavigatorWindow> windows) throws IOException {
        List<ClientNavigatorElement> listElements = new ArrayList<>();
        int elementsCount = dataStream.readInt();
        for (int i = 0; i < elementsCount; i++) {
            listElements.add(ClientNavigatorElement.deserialize(dataStream, windows));
        }
        return listElements;
    }

    public static List<ClientAbstractWindow> deserializeListClientNavigatorWindow(byte[] state) throws IOException {
        List<ClientAbstractWindow> windows = new ArrayList<>();
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(state));
        for (int i = 0; i < 3; i++) {
            windows.add(new ClientAbstractWindow(dataStream));
        }
        return windows;
    }

    public static List<ClientClass> deserializeListClientClass(byte[] state) throws IOException {

        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(state));
        List<ClientClass> classes = new ArrayList<>();
        int count = dataStream.readInt();
        for (int i = 0; i < count; i++)
            classes.add(ClientTypeSerializer.deserializeClientClass(dataStream));
        return classes;
    }

}
