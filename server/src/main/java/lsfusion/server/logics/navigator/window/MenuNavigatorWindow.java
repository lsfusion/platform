package lsfusion.server.logics.navigator.window;

import lsfusion.interop.navigator.window.WindowType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.IOException;

public class MenuNavigatorWindow extends NavigatorWindow {

    public int showLevel = 0;
    public int orientation = SwingConstants.HORIZONTAL;

    public MenuNavigatorWindow(String canonicalName, LocalizedString caption, int x, int y, int width, int height) {
        super(canonicalName, caption, x, y, width, height);
    }

    @Override
    public int getViewType() {
        return WindowType.MENU_VIEW;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(showLevel);
        outStream.writeInt(orientation);
    }
}
