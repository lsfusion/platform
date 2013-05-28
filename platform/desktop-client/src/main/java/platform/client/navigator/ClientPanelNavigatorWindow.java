package platform.client.navigator;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientPanelNavigatorWindow extends ClientNavigatorWindow {
    public int orientation;

    public ClientPanelNavigatorWindow(DataInputStream inStream) throws IOException {
        super(inStream);
        orientation = inStream.readInt();
    }

    @Override
    public PanelNavigatorView createView(INavigatorController controller) {
        return new PanelNavigatorView(this, controller);
    }
}
