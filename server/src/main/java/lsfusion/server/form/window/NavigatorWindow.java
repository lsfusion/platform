package lsfusion.server.form.window;

import lsfusion.server.logics.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class NavigatorWindow extends AbstractWindow {

    public boolean drawRoot = false;
    public boolean drawScrollBars = true;

    public NavigatorWindow(String sID, LocalizedString caption, int x, int y, int width, int height) {
        super(sID, caption, x, y, width, height);
    }

    public NavigatorWindow(String sID, LocalizedString caption, String borderConstraint) {
        super(sID, caption, borderConstraint);
    }

    public NavigatorWindow(String sID, LocalizedString caption) {
        super(sID, caption);
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(getViewType());
        super.serialize(outStream);
        outStream.writeBoolean(drawRoot);
        outStream.writeBoolean(drawScrollBars);
    }

    public abstract int getViewType();

}
