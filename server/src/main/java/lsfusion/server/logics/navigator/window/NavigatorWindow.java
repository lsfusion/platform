package lsfusion.server.logics.navigator.window;

import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class NavigatorWindow extends AbstractWindow {

    public boolean drawRoot = false;
    public boolean drawScrollBars = true;

    public NavigatorWindow(String canonicalName, LocalizedString caption, int x, int y, int width, int height) {
        super(canonicalName, caption, x, y, width, height);
    }

    public NavigatorWindow(String canonicalName, LocalizedString caption, String borderConstraint) {
        super(canonicalName, caption, borderConstraint);
    }

    public NavigatorWindow(String canonicalName, LocalizedString caption) {
        super(canonicalName, caption);
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(getViewType());
        super.serialize(outStream);
        outStream.writeBoolean(drawRoot);
        outStream.writeBoolean(drawScrollBars);
    }

    public abstract int getViewType();

}
