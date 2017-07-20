package lsfusion.client.navigator;

import javax.swing.*;
import java.awt.*;

public class IndentedIcon implements Icon {
    private final static int indentWidth = 15;

    private final Icon originalIcon;
    private final int indentOffset;

    public IndentedIcon(Icon originalIcon, int indent) {
        this.originalIcon = originalIcon;
        this.indentOffset = indent * indentWidth;
    }

    @Override
    public int getIconWidth() {
        return indentOffset + originalIcon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return originalIcon.getIconHeight();
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        originalIcon.paintIcon(c, g, x + indentOffset, y);
    }
}
