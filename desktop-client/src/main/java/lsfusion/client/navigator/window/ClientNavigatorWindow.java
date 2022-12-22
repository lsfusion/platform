package lsfusion.client.navigator.window;

import lsfusion.client.navigator.ClientNavigatorElement;
import lsfusion.client.navigator.controller.INavigatorController;
import lsfusion.client.navigator.tree.window.ClientTreeNavigatorWindow;
import lsfusion.client.navigator.view.NavigatorView;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.interop.navigator.window.WindowType.*;

public abstract class ClientNavigatorWindow extends ClientAbstractWindow {
    public List<ClientNavigatorElement> elements = new ArrayList<>();

    public boolean drawScrollBars;

    public ClientNavigatorWindow(DataInputStream inStream) throws IOException {
        super(inStream);

        drawScrollBars = inStream.readBoolean();
    }

    public abstract NavigatorView createView(INavigatorController controller);

    public static ClientNavigatorWindow deserialize(DataInputStream inStream) throws IOException {
        switch (inStream.readInt()) {
            case NULL_VIEW: return null;
            case TREE_VIEW: return new ClientTreeNavigatorWindow(inStream);
            case TOOLBAR_VIEW: return new ClientToolBarNavigatorWindow(inStream);
            case MENU_VIEW: return new ClientMenuNavigatorWindow(inStream);
            case PANEL_VIEW: return new ClientPanelNavigatorWindow(inStream);
            default:
                throw new IllegalArgumentException("Illegal view type");
        }
    }
}
