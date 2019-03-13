package lsfusion.server.logics.navigator.window;

import lsfusion.interop.navigator.WindowType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;

public class PanelNavigatorWindow extends NavigatorWindow {
    public int orientation;

    public PanelNavigatorWindow(String canonicalName, LocalizedString caption, int orientation) {
        super(canonicalName, caption);
        this.orientation = orientation;
    }

    @Override
    public int getViewType() {
        return WindowType.PANEL_VIEW;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(orientation);
    }
}
