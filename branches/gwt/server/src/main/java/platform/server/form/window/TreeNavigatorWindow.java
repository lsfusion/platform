package platform.server.form.window;

import platform.interop.NavigatorWindowType;

import java.io.DataOutputStream;
import java.io.IOException;

public class TreeNavigatorWindow extends NavigatorWindow{
    public TreeNavigatorWindow(String sID, String caption, int x, int y, int width, int height) {
        super(sID, caption, x, y, width, height);
    }

    @Override
    public int getViewType() {
        return NavigatorWindowType.TREE_VIEW;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
    }
}
