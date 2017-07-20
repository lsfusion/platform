package lsfusion.server.form.window;

import lsfusion.interop.AbstractWindowType;
import lsfusion.server.logics.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;

public class TreeNavigatorWindow extends NavigatorWindow {

    public TreeNavigatorWindow(String sID, LocalizedString caption, int x, int y, int width, int height) {
        super(sID, caption, x, y, width, height);
    }

    public TreeNavigatorWindow(String sID, LocalizedString caption) {
        super(sID, caption);
    }

    @Override
    public int getViewType() {
        return AbstractWindowType.TREE_VIEW;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
    }
}
