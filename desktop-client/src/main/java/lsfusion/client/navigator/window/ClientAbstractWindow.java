package lsfusion.client.navigator.window;

import lsfusion.client.navigator.ClientNavigatorElement;
import lsfusion.interop.form.remote.serialization.SerializationUtil;
import lsfusion.interop.navigator.window.WindowType;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ClientAbstractWindow<C extends JComponent> implements Serializable {
    public List<ClientNavigatorElement> elements = new ArrayList<>();

    public String canonicalName;
    public String caption;
    public int position;

    public int x;
    public int y;
    public int width;
    public int height;

    public String borderConstraint;

    public boolean titleShown;
    public boolean visible;
    
    public String elementClass;
    
    public boolean autoSize;

    // the wire model both clients read: ClientNavigatorToGwtConverter carries these on to the web client, while the
    // desktop client draws its regular toolbar and its regular tabs, and ignores them
    public String custom;
    public boolean react;
    // one form at a time - what a FORMS window does unless it is TABBED. The desktop client draws forms only in
    // its own forms window, so it reads this and ignores it
    public boolean single;
    public int closeDelay;
    public boolean vertical; // the window's own orientation; AUTOSIZE hugs across it

    public ClientAbstractWindow(DataInputStream inStream) throws IOException {
        canonicalName = inStream.readUTF();
        caption = inStream.readUTF();

        position = inStream.readInt();
        if (position == WindowType.DOCKING_POSITION) {
            x = inStream.readInt();
            y = inStream.readInt();
            width = inStream.readInt();
            height = inStream.readInt();
        }
        if (position == WindowType.BORDER_POSITION) {
            borderConstraint = inStream.readUTF();
        }

        titleShown = inStream.readBoolean();
        visible = inStream.readBoolean();
        
        if (inStream.readBoolean()) {
            elementClass = inStream.readUTF();
        }
        
        autoSize = inStream.readBoolean();

        custom = SerializationUtil.readString(inStream);
        react = inStream.readBoolean();
        single = inStream.readBoolean();
        closeDelay = inStream.readInt();
        vertical = inStream.readBoolean();
    }

    // System.forms - the window this client draws forms in, since it draws them in one. Read off the name, the way
    // ClientNavigatorWindow reads its own
    public boolean isSystemForms() {
        return "System.forms".equals(canonicalName);
    }

    @Override
    public int hashCode() {
        return canonicalName.hashCode();
    }

    @Override
    public String toString() {
        return "Window[canonicalName:" + canonicalName + ", caption: " + caption + "]";
    }
}
