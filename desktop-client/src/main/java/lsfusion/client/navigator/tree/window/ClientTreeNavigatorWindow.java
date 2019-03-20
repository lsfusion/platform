package lsfusion.client.navigator.tree.window;

import lsfusion.client.navigator.controller.INavigatorController;
import lsfusion.client.navigator.tree.view.TreeNavigatorView;
import lsfusion.client.navigator.window.ClientNavigatorWindow;

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
