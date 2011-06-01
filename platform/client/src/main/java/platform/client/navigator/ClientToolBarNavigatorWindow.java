package platform.client.navigator;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientToolBarNavigatorWindow extends ClientNavigatorWindow {
    public int type;
    public boolean showSelect;

    public ClientToolBarNavigatorWindow(DataInputStream inStream) throws IOException {
        super(inStream);
        type = inStream.readInt();
        showSelect = inStream.readBoolean();
    }

    @Override
    public ToolBarNavigatorView getView(INavigatorController controller) {
        return new ToolBarNavigatorView(this, controller);
    }
}
