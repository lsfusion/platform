package platform.interop.form.layout;

import java.util.Arrays;
import java.util.List;

public class ContainerType {
    public final static byte CONTAINERH = 0;
    public final static byte CONTAINERV = 1;
    public final static byte CONTAINERVH = 2;
    public final static byte TABBED_PANE = 3;
    public final static byte SPLIT_PANE_VERTICAL = 4;
    public final static byte SPLIT_PANE_HORIZONTAL = 5;
    public final static byte CONTAINER = 6;

    public static List<String> getTypeNamesList() {
        return Arrays.asList("CONTAINERH", "CONTAINERV", "CONTAINERVH", "TABBED PANE", "SPLIT PANE VERTICAL", "SPLIT PANE HORIZONTAL");
    }
}
