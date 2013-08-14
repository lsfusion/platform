package lsfusion.server.logics.scripted;

import lsfusion.interop.form.layout.Alignment;

import javax.swing.*;

public class AlignmentUtils {

    public static float asVerticalToolbarAlign(Alignment vAlign) {
        switch (vAlign) {
            case LEADING: return JToolBar.TOP_ALIGNMENT;
            case CENTER: return JToolBar.CENTER_ALIGNMENT;
            case TRAILING: return JToolBar.BOTTOM_ALIGNMENT;
        }
        throw new IllegalStateException("wrong enum value");
    }

    public static int asVerticalTextPosition(Alignment vAlign) {
        switch (vAlign) {
            case LEADING: return SwingConstants.TOP;
            case CENTER: return SwingConstants.CENTER;
            case TRAILING: return SwingConstants.BOTTOM;
        }
        throw new IllegalStateException("wrong enum value");
    }

    public static float asHorizontalToolbarAlign(Alignment hAlign) {
        switch (hAlign) {
            case LEADING: return JToolBar.LEFT_ALIGNMENT;
            case CENTER: return JToolBar.CENTER_ALIGNMENT;
            case TRAILING: return JToolBar.RIGHT_ALIGNMENT;
        }
        throw new IllegalStateException("wrong enum value");
    }

    public static int asHorizontalTextPosition(Alignment hAlign) {
        switch (hAlign) {
            case LEADING: return SwingUtilities.LEADING;
            case CENTER: return SwingConstants.CENTER;
            case TRAILING: return SwingConstants.RIGHT;
        }
        throw new IllegalStateException("wrong enum value");
    }
}
