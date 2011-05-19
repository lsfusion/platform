package platform.client.navigator;

import platform.base.identity.IdentityObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class ClientNavigatorWindow extends IdentityObject implements Serializable {
    public String caption;
    public String sid;
    public List<ClientNavigatorElement> elements = new ArrayList<ClientNavigatorElement>();
    public static Map<String, ClientNavigatorWindow> sidToWindow = new HashMap<String, ClientNavigatorWindow>();
    public int x;
    public int y;
    public int width;
    public int height;

    public ClientNavigatorWindow(int id, String sid, String caption, int x, int y, int width, int height) {
        super(id);
        this.sid = sid;
        this.caption = caption;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }


    public static ClientNavigatorWindow deserialize(DataInputStream inStream) throws IOException {
        int id = inStream.readInt();
        if (id == -1) {
            return null;
        } else {
            String caption = inStream.readUTF();
            String sid = inStream.readUTF();
            int x = inStream.readInt();
            int y = inStream.readInt();
            int width = inStream.readInt();
            int height = inStream.readInt();
            if (sidToWindow.containsKey(sid)) {
                return sidToWindow.get(sid);
            } else {
                ClientNavigatorWindow clientNavigatorWindow = new ClientNavigatorWindow(id, sid, caption, x, y, width, height);
                sidToWindow.put(sid, clientNavigatorWindow);
                return clientNavigatorWindow;
            }
        }
    }

    @Override
    public int hashCode() {
        return sid.hashCode();
    }
}
