package platform.server.form.window;

import platform.interop.NavigatorWindowType;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.IOException;

public class ToolBarNavigatorWindow extends NavigatorWindow {

    public int type;
    public boolean showSelect = true;

    public int verticalTextPosition = SwingConstants.BOTTOM;
    public int horizontalTextPosition = SwingConstants.CENTER;

    public int verticalAlignment = SwingConstants.CENTER;
    public int horizontalAlignment = SwingConstants.CENTER;

    public ToolBarNavigatorWindow(int type, String sID, String caption, int x, int y, int width, int height) {
        super(sID, caption, x, y, width, height);

        this.type = type;
        if (this.type == JToolBar.VERTICAL) {
            verticalTextPosition = SwingConstants.CENTER;
            horizontalTextPosition = SwingConstants.TRAILING;

            horizontalAlignment = SwingConstants.LEFT;
        }
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

        outStream.writeInt(verticalTextPosition);
        outStream.writeInt(horizontalTextPosition);

        outStream.writeInt(verticalAlignment);
        outStream.writeInt(horizontalAlignment);
    }
}
