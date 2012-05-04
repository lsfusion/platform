package platform.server.form.window;

import platform.interop.AbstractWindowType;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.IOException;

public class MenuNavigatorWindow extends NavigatorWindow {

    public int showLevel = 0;
    public int orientation = SwingConstants.HORIZONTAL;

    public MenuNavigatorWindow(String sID, String caption, int x, int y, int width, int height) {
        super(sID, caption, x, y, width, height);
    }

    @Override
    public int getViewType() {
        return AbstractWindowType.MENU_VIEW;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(showLevel);
        outStream.writeInt(orientation);
    }
}
