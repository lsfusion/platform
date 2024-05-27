package lsfusion.server.logics.navigator.window;

import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.IOException;

public class NavigatorWindow extends AbstractWindow {

    public int type;
    public boolean showSelect = true;

    public int verticalTextPosition = SwingConstants.BOTTOM;
    public int horizontalTextPosition = SwingConstants.CENTER;

    public int verticalAlignment = SwingConstants.CENTER;
    public int horizontalAlignment = SwingConstants.CENTER;

    public float alignmentY = JToolBar.TOP_ALIGNMENT;
    public float alignmentX = JToolBar.LEFT_ALIGNMENT;

    public NavigatorWindow(int type, String canonicalName, LocalizedString caption, int x, int y, int width, int height) {
        super(canonicalName, caption, x, y, width, height);

        setType(type);
    }

    public NavigatorWindow(int type, String canonicalName, LocalizedString caption, String borderConstraint) {
        super(canonicalName, caption, borderConstraint);

        setType(type);
    }

    public NavigatorWindow(int type, String canonicalName, LocalizedString caption) {
        super(canonicalName, caption);

        setType(type);
    }

    private void setType(int type) {
        this.type = type;
        if (this.type == JToolBar.VERTICAL) {
            verticalTextPosition = SwingConstants.CENTER;
            horizontalTextPosition = SwingConstants.TRAILING;

            horizontalAlignment = SwingConstants.LEFT;
        }
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

        outStream.writeFloat(alignmentY);
        outStream.writeFloat(alignmentX);

        outStream.writeBoolean(drawScrollBars);
    }
}
