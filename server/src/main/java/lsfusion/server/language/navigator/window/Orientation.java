package lsfusion.server.language.navigator.window;

import javax.swing.*;

public enum Orientation {
    HORIZONTAL, VERTICAL;

    public int asMenuOrientation() {
        switch (this) {
            case HORIZONTAL: return SwingConstants.HORIZONTAL;
            case VERTICAL: return SwingConstants.VERTICAL;
        }
        throw new IllegalStateException("wrong enum value");
    }

    public int asToolbarOrientation() {
        switch (this) {
            case HORIZONTAL: return SwingConstants.HORIZONTAL;
            case VERTICAL: return SwingConstants.VERTICAL;
        }
        throw new IllegalStateException("wrong enum value");
    }


}
