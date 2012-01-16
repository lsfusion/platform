package platform.server.logics.scripted;

import javax.swing.*;

public enum HAlign {
    LEFT {
        @Override
        public float asToolbarAlign() {
            return JToolBar.LEFT_ALIGNMENT;
        }

        @Override
        public int asComponentAlign() {
            return SwingConstants.LEFT;
        }

        @Override
        public int asTextPosition() {
            return SwingUtilities.LEADING;
        }
    },
    CENTER {
        @Override
        public float asToolbarAlign() {
            return JToolBar.CENTER_ALIGNMENT;
        }

        @Override
        public int asComponentAlign() {
            return SwingUtilities.CENTER;
        }

        @Override
        public int asTextPosition() {
            return SwingUtilities.CENTER;
        }
    },
    RIGHT {
        @Override
        public float asToolbarAlign() {
            return JToolBar.RIGHT_ALIGNMENT;
        }

        @Override
        public int asComponentAlign() {
            return SwingUtilities.RIGHT;
        }

        @Override
        public int asTextPosition() {
            return SwingUtilities.TRAILING;
        }
    };

    public abstract float asToolbarAlign();
    public abstract int asComponentAlign();
    public abstract int asTextPosition();
}
