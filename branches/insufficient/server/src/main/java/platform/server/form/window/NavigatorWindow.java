package platform.server.form.window;

import platform.base.identity.IdentityObject;
import platform.server.logics.BusinessLogics;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NavigatorWindow extends IdentityObject {

    public String caption = "";
    private int x;
    private int y;
    private int width;
    private int height;

    public NavigatorWindow(String sID, String caption, int x, int y, int width, int height) {
        this.sID = sID;
        setID(BusinessLogics.generateStaticNewID());
        this.caption = caption;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static void serialize(DataOutputStream outStream, NavigatorWindow window) throws IOException {
        if (window == null) {
            outStream.writeInt(-1);
        } else {
            outStream.writeInt(window.getID());
            outStream.writeUTF(window.caption);
            outStream.writeUTF(window.getSID());
            outStream.writeInt(window.x);
            outStream.writeInt(window.y);
            outStream.writeInt(window.width);
            outStream.writeInt(window.height);
        }
    }

}
