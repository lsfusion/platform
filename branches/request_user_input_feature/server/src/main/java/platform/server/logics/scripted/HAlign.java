package platform.server.logics.scripted;

import javax.swing.*;

public enum HAlign {
    LEFT, CENTER, RIGHT;

    public float asToolbarAlign() {
        switch (this) {
            case LEFT: return JToolBar.LEFT_ALIGNMENT;
            case CENTER: return JToolBar.CENTER_ALIGNMENT;
            case RIGHT: return JToolBar.RIGHT_ALIGNMENT;
        }
        throw new IllegalStateException("wrong enum value");
    }

    public int asComponentAlign() {
        switch (this) {
            case LEFT: return SwingConstants.LEFT;
            case CENTER: return SwingConstants.CENTER;
            case RIGHT: return SwingConstants.RIGHT;
        }
        throw new IllegalStateException("wrong enum value");
    }

    public int asTextPosition() {
        switch (this) {
            case LEFT: return SwingUtilities.LEADING;
            case CENTER: return SwingConstants.CENTER;
            case RIGHT: return SwingConstants.RIGHT;
        }
        throw new IllegalStateException("wrong enum value");
    }
}
