package platform.server.logics.scripted;

import java.awt.*;

public enum BorderPosition {
    LEFT {
        @Override
        public String asLayoutConstraint() {
            return BorderLayout.WEST;
        }
    },
    RIGHT {
        @Override
        public String asLayoutConstraint() {
            return BorderLayout.EAST;
        }
    },
    TOP {
        @Override
        public String asLayoutConstraint() {
            return BorderLayout.NORTH;
        }
    },
    BOTTOM {
        @Override
        public String asLayoutConstraint() {
            return BorderLayout.SOUTH;
        }
    };

    public abstract String asLayoutConstraint();
}
