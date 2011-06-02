package platform.client.navigator;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientMenuNavigatorWindow extends ClientNavigatorWindow {
    public int showLevel;

    public ClientMenuNavigatorWindow(DataInputStream inStream) throws IOException {
        super(inStream);
        showLevel = inStream.readInt();
    }

    @Override
    public MenuNavigatorView getView(INavigatorController controller) {
        return new MenuNavigatorView(this, controller);
    }
}
