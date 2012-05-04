package platform.interop.form.layout;

import java.util.Arrays;
import java.util.List;

public class ContainerType {
    public final static byte CONTAINER = 0;
    public final static byte TABBED_PANE = 1;
    public final static byte SPLIT_PANE_VERTICAL = 2;
    public final static byte SPLIT_PANE_HORIZONTAL = 3;

    public static List<String> getTypeNamesList() {
        return Arrays.asList("CONTAINER", "TABBED PANE", "SPLIT PANE VERTICAL", "SPLIT PANE HORIZONTAL");
    }
}
