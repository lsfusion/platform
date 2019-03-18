package lsfusion.client.navigator.window;

import lsfusion.client.navigator.INavigatorController;
import lsfusion.client.navigator.view.MenuNavigatorView;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientMenuNavigatorWindow extends ClientNavigatorWindow {
    public int showLevel;
    public int orientation;

    public ClientMenuNavigatorWindow(DataInputStream inStream) throws IOException {
        super(inStream);
        showLevel = inStream.readInt();
        orientation = inStream.readInt();
    }

    @Override
    public MenuNavigatorView createView(INavigatorController controller) {
        return new MenuNavigatorView(this, controller);
    }
}
