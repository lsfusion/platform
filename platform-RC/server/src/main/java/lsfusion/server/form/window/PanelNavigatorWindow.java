package lsfusion.server.form.window;

import lsfusion.interop.AbstractWindowType;
import lsfusion.server.logics.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;

public class PanelNavigatorWindow extends NavigatorWindow {
    public int orientation;

    public PanelNavigatorWindow(int orientation, String sid, LocalizedString caption) {
        super(sid, caption);
        this.orientation = orientation;
    }

    @Override
    public int getViewType() {
        return AbstractWindowType.PANEL_VIEW;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(orientation);
    }
}
