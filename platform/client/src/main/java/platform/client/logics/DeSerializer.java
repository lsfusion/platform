package platform.client.logics;

import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.client.navigator.ClientAbstractWindow;
import platform.client.navigator.ClientNavigatorElement;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeSerializer {

    public static List<ClientNavigatorElement> deserializeListClientNavigatorElementWithChildren(byte[] state) throws IOException {
        List<ClientNavigatorElement> elements = deserializeListClientNavigatorElement(state);

        Map<String, ClientNavigatorElement> elementsMap = new HashMap<String, ClientNavigatorElement>();
        for (ClientNavigatorElement element : elements) {
            elementsMap.put(element.getSID(), element);
        }

        for (ClientNavigatorElement element : elements) {
            for (String s : element.childrenSid) {
                ClientNavigatorElement child = elementsMap.get(s);
                element.children.add(child);
            }
        }

        return elements;
    }

    public static List<ClientNavigatorElement> deserializeListClientNavigatorElement(byte[] state) throws IOException {
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(state));
        List<ClientNavigatorElement> listElements = new ArrayList<ClientNavigatorElement>();
        int count = dataStream.readInt();
        for (int i = 0; i < count; i++)
            listElements.add(ClientNavigatorElement.deserialize(dataStream));
        return listElements;
    }

    public static List<ClientAbstractWindow> deserializeListClientNavigatorWindow(byte[] state) throws IOException {
        List<ClientAbstractWindow> windows = new ArrayList<ClientAbstractWindow>();
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(state));
        for (int i = 0; i < 5; i++) {
            windows.add(new ClientAbstractWindow(dataStream));
        }
        return windows;
    }

    public static List<ClientClass> deserializeListClientClass(byte[] state) throws IOException {

        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(state));
        List<ClientClass> classes = new ArrayList<ClientClass>();
        int count = dataStream.readInt();
        for (int i = 0; i < count; i++)
            classes.add(ClientTypeSerializer.deserializeClientClass(dataStream));
        return classes;
    }

}
