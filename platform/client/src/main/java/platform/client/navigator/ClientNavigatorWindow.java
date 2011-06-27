package platform.client.navigator;

import platform.base.identity.IdentityObject;
import platform.interop.NavigatorWindowType;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public abstract class ClientNavigatorWindow extends IdentityObject implements Serializable {
    public String caption;
    public String sid;
    public List<ClientNavigatorElement> elements = new ArrayList<ClientNavigatorElement>();
    public static Map<String, ClientNavigatorWindow> sidToWindow = new HashMap<String, ClientNavigatorWindow>();

    public int position;

    public int x;
    public int y;
    public int width;
    public int height;

    public String borderConstraint;

    public int type;
    public boolean titleShown;
    public boolean drawRoot;

    public ClientNavigatorWindow(DataInputStream inStream) throws IOException {
        super(inStream.readInt());
        caption = inStream.readUTF();
        sid = inStream.readUTF();

        position = inStream.readInt();
        if (position == NavigatorWindowType.DOCKING_POSITION) {
            x = inStream.readInt();
            y = inStream.readInt();
            width = inStream.readInt();
            height = inStream.readInt();
        }
        if (position == NavigatorWindowType.BORDER_POSITION) {
            borderConstraint = inStream.readUTF();
        }

        titleShown = inStream.readBoolean();
        drawRoot =  inStream.readBoolean();
    }


    public static ClientNavigatorWindow deserialize(DataInputStream inStream) throws IOException {
        int type = inStream.readInt();
        ClientNavigatorWindow result;
        switch (type) {
            case NavigatorWindowType.NULL_VIEW: {
                result = null;
                break;
            }
            case NavigatorWindowType.TREE_VIEW: {
                result = new ClientTreeNavigatorWindow(inStream);
                break;
            }
            case NavigatorWindowType.TOOLBAR_VIEW: {
                result = new ClientToolBarNavigatorWindow(inStream);
                break;
            }
            case NavigatorWindowType.MENU_VIEW: {
                result = new ClientMenuNavigatorWindow(inStream);
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

    @Override
    public int hashCode() {
        return sid.hashCode();
    }

    public abstract NavigatorView getView(INavigatorController controller);

}
