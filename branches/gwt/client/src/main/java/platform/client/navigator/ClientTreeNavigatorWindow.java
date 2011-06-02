package platform.client.navigator;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientTreeNavigatorWindow extends ClientNavigatorWindow{

    public ClientTreeNavigatorWindow(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public TreeNavigatorView getView(INavigatorController controller) {
        return new TreeNavigatorView(this, controller);
    }
}
