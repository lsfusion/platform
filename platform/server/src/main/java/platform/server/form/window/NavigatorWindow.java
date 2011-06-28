package platform.server.form.window;

import platform.interop.AbstractWindowType;
import platform.server.logics.BusinessLogics;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class NavigatorWindow extends AbstractWindow {

    public boolean drawRoot = false;

    public NavigatorWindow(String sID, String caption, int x, int y, int width, int height) {
        super(sID, caption, x, y, width, height);
    }

    public NavigatorWindow(String sID, String caption, String borderConstraint) {
        super(sID, caption, borderConstraint);
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(getViewType());
        super.serialize(outStream);
        outStream.writeBoolean(drawRoot);
    }

    public abstract int getViewType();

}
