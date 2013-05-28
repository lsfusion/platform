package platform.client.navigator;

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
