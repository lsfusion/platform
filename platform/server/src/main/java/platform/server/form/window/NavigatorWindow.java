package platform.server.form.window;

import platform.base.identity.IdentityObject;
import platform.interop.NavigatorWindowType;
import platform.server.logics.BusinessLogics;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class NavigatorWindow extends IdentityObject {

    public String caption = "";

    public int position;

    public int x;
    public int y;
    public int width;
    public int height;

    public String borderConstraint;

    public boolean titleShown = true;
    public boolean drawRoot = false;

    public NavigatorWindow(String sID, String caption, int x, int y, int width, int height) {
        this(sID, caption);

        this.position = NavigatorWindowType.DOCKING_POSITION;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public NavigatorWindow(String sID, String caption, String borderConstraint) {
        this(sID, caption);

        this.position = NavigatorWindowType.BORDER_POSITION;
        this.borderConstraint = borderConstraint;
    }

    private NavigatorWindow(String SID, String caption) {
        this.sID = sID;
        setID(BusinessLogics.generateStaticNewID());
        this.caption = caption;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(getViewType());
        outStream.writeInt(getID());
        outStream.writeUTF(caption);
        outStream.writeUTF(getSID());

        outStream.writeInt(position);
        if (position == NavigatorWindowType.DOCKING_POSITION) {
            outStream.writeInt(x);
            outStream.writeInt(y);
            outStream.writeInt(width);
            outStream.writeInt(height);
        }
        if (position == NavigatorWindowType.BORDER_POSITION) {
            outStream.writeUTF(borderConstraint);
        }

        outStream.writeBoolean(titleShown);
        outStream.writeBoolean(drawRoot);
    }

    public void changePosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }


    public abstract int getViewType();

}
