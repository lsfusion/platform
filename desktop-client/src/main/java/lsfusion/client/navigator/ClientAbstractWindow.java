package lsfusion.client.navigator;

import lsfusion.interop.navigator.WindowType;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;

public class ClientAbstractWindow<C extends JComponent> implements Serializable {
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
