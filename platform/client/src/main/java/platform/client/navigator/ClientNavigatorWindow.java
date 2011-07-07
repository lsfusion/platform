package platform.client.navigator;

import platform.interop.AbstractWindowType;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public abstract class ClientNavigatorWindow extends ClientAbstractWindow {

    public List<ClientNavigatorElement> elements = new ArrayList<ClientNavigatorElement>();
    // конечно не совсем правильно хранить sidToWindow static'ом
    // правильно было бы наверное хранить в DockableMainFrame, но тогда пришлось бы передавать во все сериализации
    public static Map<String, ClientNavigatorWindow> sidToWindow = new HashMap<String, ClientNavigatorWindow>();

    public int type;
    public boolean drawRoot;
    public boolean drawScrollBars;

    public ClientNavigatorWindow(DataInputStream inStream) throws IOException {
        super(inStream);

        drawRoot = inStream.readBoolean();
        drawScrollBars = inStream.readBoolean();
    }


    public static ClientNavigatorWindow deserialize(DataInputStream inStream) throws IOException {
        int type = inStream.readInt();
        ClientNavigatorWindow result;
        switch (type) {
            case AbstractWindowType.NULL_VIEW: {
                result = null;
                break;
            }
            case AbstractWindowType.TREE_VIEW: {
                result = new ClientTreeNavigatorWindow(inStream);
                break;
            }
            case AbstractWindowType.TOOLBAR_VIEW: {
                result = new ClientToolBarNavigatorWindow(inStream);
                break;
            }
            case AbstractWindowType.MENU_VIEW: {
                result = new ClientMenuNavigatorWindow(inStream);
                break;
            }
            case AbstractWindowType.PANEL_VIEW: {
                result = new ClientPanelNavigatorWindow(inStream);
                break;
            }
            default:
                throw new IllegalArgumentException("Illegal view type");
        }
        if (result != null) {
            if (sidToWindow.containsKey(result.getSID())) {
                result = sidToWindow.get(result.getSID());
            } else {
                sidToWindow.put(result.getSID(), result);
            }
        }
        return result;
    }

    public abstract NavigatorView getView(INavigatorController controller);

}
