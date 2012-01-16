package platform.server.logics.scripted;

import javax.swing.*;

public enum Orientation {
    HORIZONTAL {
        @Override
        public int asToolbarOrientation() {
            return SwingConstants.HORIZONTAL;
        }
    },
    VERTICAL {
        @Override
        public int asToolbarOrientation() {
            return SwingConstants.VERTICAL;
        }
    };

    public abstract int asToolbarOrientation();
}
