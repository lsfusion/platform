package lsfusion.server.language.navigator.window;

import lsfusion.interop.base.view.FlexAlignment;

import javax.swing.*;

public class AlignmentUtils {

    public static float asVerticalToolbarAlign(FlexAlignment vAlign) {
        switch (vAlign) {
            case START: return JToolBar.TOP_ALIGNMENT;
            case CENTER: return JToolBar.CENTER_ALIGNMENT;
            case END: return JToolBar.BOTTOM_ALIGNMENT;
        }
        throw new IllegalStateException("wrong enum value");
    }

    public static int asVerticalTextPosition(FlexAlignment vAlign) {
        switch (vAlign) {
            case START: return SwingConstants.TOP;
            case CENTER: return SwingConstants.CENTER;
            case END: return SwingConstants.BOTTOM;
        }
        throw new IllegalStateException("wrong enum value");
    }

    public static float asHorizontalToolbarAlign(FlexAlignment hAlign) {
        switch (hAlign) {
            case START: return JToolBar.LEFT_ALIGNMENT;
            case CENTER: return JToolBar.CENTER_ALIGNMENT;
            case END: return JToolBar.RIGHT_ALIGNMENT;
        }
        throw new IllegalStateException("wrong enum value");
    }

    public static int asHorizontalTextPosition(FlexAlignment hAlign) {
        switch (hAlign) {
            case START: return SwingUtilities.LEADING;
            case CENTER: return SwingConstants.CENTER;
            case END: return SwingConstants.RIGHT;
        }
        throw new IllegalStateException("wrong enum value");
    }
}
