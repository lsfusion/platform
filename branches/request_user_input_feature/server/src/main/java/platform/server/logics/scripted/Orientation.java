package platform.server.logics.scripted;

import javax.swing.*;

public enum Orientation {
    HORIZONTAL {
        @Override
        public int asMenuOrientation() {
            return SwingConstants.HORIZONTAL;
        }

        @Override
        public int asToolbarOrientation() {
            return SwingConstants.HORIZONTAL;
        }
    },
    VERTICAL {
        @Override
        public int asMenuOrientation() {
            return SwingConstants.VERTICAL;
        }

        @Override
        public int asToolbarOrientation() {
            return SwingConstants.VERTICAL;
        }
    };

    public abstract int asToolbarOrientation();
    public abstract int asMenuOrientation();
}
