package platform.gwt.base.shared;

public class GContainerType {
    public final static byte CONTAINER = 0;
    public final static byte TABBED_PANE = 1;
    public final static byte SPLIT_PANE_VERTICAL = 2;
    public final static byte SPLIT_PANE_HORIZONTAL = 3;

    public static boolean isContainer(byte value) {
        return value == CONTAINER;
    }

    public static boolean isSplitPane(byte value) {
        return value == SPLIT_PANE_HORIZONTAL || value == SPLIT_PANE_VERTICAL;
    }

    public static boolean isTabbedPane(byte value) {
        return value == TABBED_PANE;
    }
}
