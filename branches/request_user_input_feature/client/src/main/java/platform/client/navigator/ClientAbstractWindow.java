package platform.client.navigator;

import platform.base.identity.IdentityObject;
import platform.interop.AbstractWindowType;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;

public class ClientAbstractWindow<C extends JComponent> extends IdentityObject implements Serializable {
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
        super(inStream.readInt());
        caption = inStream.readUTF();
        sID = inStream.readUTF();

        position = inStream.readInt();
        if (position == AbstractWindowType.DOCKING_POSITION) {
            x = inStream.readInt();
            y = inStream.readInt();
            width = inStream.readInt();
            height = inStream.readInt();
        }
        if (position == AbstractWindowType.BORDER_POSITION) {
            borderConstraint = inStream.readUTF();
        }

        titleShown = inStream.readBoolean();
        visible = inStream.readBoolean();
    }

    @Override
    public int hashCode() {
        return sID.hashCode();
    }
}
