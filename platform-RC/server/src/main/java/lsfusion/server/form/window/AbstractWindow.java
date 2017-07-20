package lsfusion.server.form.window;

import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.AbstractWindowType;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;

public class AbstractWindow extends IdentityObject {

    public LocalizedString caption = LocalizedString.create("");

    public int position;

    public int x;
    public int y;
    public int width;
    public int height;

    public String borderConstraint;

    public boolean titleShown = true;

    public boolean visible = true;

    public AbstractWindow(String sID, LocalizedString caption, int x, int y, int width, int height) {
        this(sID, caption);

        setDockPosition(x, y, width, height);
    }

    public AbstractWindow(String sID, LocalizedString caption, String borderConstraint) {
        this(sID, caption);

        setBorderPosition(borderConstraint);
    }

    public AbstractWindow(String sID, LocalizedString caption) {
        this.sID = sID;
        setID(BaseLogicsModule.generateStaticNewID());
        this.caption = caption;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(getID());
        outStream.writeUTF(ThreadLocalContext.localize(caption));
        outStream.writeUTF(getSID());

        outStream.writeInt(position);
        if (position == AbstractWindowType.DOCKING_POSITION) {
            outStream.writeInt(x);
            outStream.writeInt(y);
            outStream.writeInt(width);
            outStream.writeInt(height);
        }
        if (position == AbstractWindowType.BORDER_POSITION) {
            outStream.writeUTF(borderConstraint);
        }

        outStream.writeBoolean(titleShown);
        outStream.writeBoolean(visible);
    }

    public void setDockPosition(int x, int y, int width, int height) {
        this.position = AbstractWindowType.DOCKING_POSITION;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    private void setBorderPosition(String borderConstraint) {
        this.position = AbstractWindowType.BORDER_POSITION;
        this.borderConstraint = borderConstraint;
    }
}
