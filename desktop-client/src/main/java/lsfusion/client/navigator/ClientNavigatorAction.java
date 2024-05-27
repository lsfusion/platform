package lsfusion.client.navigator;

import lsfusion.client.navigator.window.ClientNavigatorWindow;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

public class ClientNavigatorAction extends ClientNavigatorElement {

    public ClientNavigatorAction(DataInputStream inStream, Map<String, ClientNavigatorWindow> windows) throws IOException {
        super(inStream, windows);
    }
}
