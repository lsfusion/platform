package lsfusion.server.language.navigator.window;

import lsfusion.interop.form.design.Alignment;

import javax.swing.*;

public class AlignmentUtils {

    public static float asVerticalToolbarAlign(Alignment vAlign) {
        switch (vAlign) {
            case START: return JToolBar.TOP_ALIGNMENT;
            case CENTER: return JToolBar.CENTER_ALIGNMENT;
            case END: return JToolBar.BOTTOM_ALIGNMENT;
        }
        throw new IllegalStateException("wrong enum value");
    }

    public static int asVerticalTextPosition(Alignment vAlign) {
        switch (vAlign) {
            case START: return SwingConstants.TOP;
            case CENTER: return SwingConstants.CENTER;
            case END: return SwingConstants.BOTTOM;
        }
        throw new IllegalStateException("wrong enum value");
    }

    public static float asHorizontalToolbarAlign(Alignment hAlign) {
        switch (hAlign) {
            case START: return JToolBar.LEFT_ALIGNMENT;
            case CENTER: return JToolBar.CENTER_ALIGNMENT;
            case END: return JToolBar.RIGHT_ALIGNMENT;
        }
        throw new IllegalStateException("wrong enum value");
    }

    public static int asHorizontalTextPosition(Alignment hAlign) {
        switch (hAlign) {
            case START: return SwingUtilities.LEADING;
            case CENTER: return SwingConstants.CENTER;
            case END: return SwingConstants.RIGHT;
        }
        throw new IllegalStateException("wrong enum value");
    }
}
