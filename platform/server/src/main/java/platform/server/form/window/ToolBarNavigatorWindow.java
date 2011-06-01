package platform.server.form.window;

import platform.interop.NavigatorWindowType;

import java.io.DataOutputStream;
import java.io.IOException;

public class ToolBarNavigatorWindow extends NavigatorWindow {

    public final static int HORIZONTAL = 0;
    public final static int VERTICAL = 1;
    public int type;
    public boolean showSelect = true;

    public ToolBarNavigatorWindow(String sID, String caption, int x, int y, int width, int height) {
        super(sID, caption, x, y, width, height);
    }

    @Override
    public int getViewType() {
        return NavigatorWindowType.TOOLBAR_VIEW;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(type);
        outStream.writeBoolean(showSelect);
    }
}
