package lsfusion.server.logics.scripted;

import java.awt.*;

public enum BorderPosition {
    LEFT, RIGHT, TOP, BOTTOM;

    public String asLayoutConstraint() {
        switch (this) {
            case LEFT: return BorderLayout.WEST;
            case RIGHT: return BorderLayout.EAST;
            case TOP: return BorderLayout.NORTH;
            case BOTTOM: return BorderLayout.SOUTH;
        }
        throw new IllegalStateException("wrong enum value");
    }
}
