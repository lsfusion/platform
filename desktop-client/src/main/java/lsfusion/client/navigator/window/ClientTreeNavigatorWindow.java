package lsfusion.client.navigator.window;

import lsfusion.client.navigator.INavigatorController;
import lsfusion.client.navigator.TreeNavigatorView;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientTreeNavigatorWindow extends ClientNavigatorWindow {

    public ClientTreeNavigatorWindow(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public TreeNavigatorView createView(INavigatorController controller) {
        return new TreeNavigatorView(this, controller);
    }
}
