package lsfusion.server.logics.scripted;

import javax.swing.*;

public enum VAlign {
    TOP, CENTER, BOTTOM;

    public float asToolbarAlign() {
        switch (this) {
            case TOP: return JToolBar.TOP_ALIGNMENT;
            case CENTER: return JToolBar.CENTER_ALIGNMENT;
            case BOTTOM: return JToolBar.BOTTOM_ALIGNMENT;
        }
        throw new IllegalStateException("wrong enum value");
    }

    public int asTextPosition() {
        switch (this) {
            case TOP: return SwingConstants.TOP;
            case CENTER: return SwingConstants.CENTER;
            case BOTTOM: return SwingConstants.BOTTOM;
        }
        throw new IllegalStateException("wrong enum value");
    }
}
