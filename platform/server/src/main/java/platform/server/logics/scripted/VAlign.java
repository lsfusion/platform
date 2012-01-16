package platform.server.logics.scripted;

import javax.swing.*;

public enum VAlign {
    TOP {
        @Override
        public float asToolbarAlign() {
            return JToolBar.TOP_ALIGNMENT;
        }

        @Override
        public int asTextPosition() {
            return SwingUtilities.TOP;
        }
    },
    CENTER {
        @Override
        public float asToolbarAlign() {
            return JToolBar.CENTER_ALIGNMENT;
        }

        @Override
        public int asTextPosition() {
            return SwingUtilities.CENTER;
        }
    },
    BOTTOM {
        @Override
        public float asToolbarAlign() {
            return JToolBar.BOTTOM_ALIGNMENT;
        }

        @Override
        public int asTextPosition() {
            return SwingUtilities.BOTTOM;
        }
    };

    public abstract float asToolbarAlign();
    public abstract int asTextPosition();
}
