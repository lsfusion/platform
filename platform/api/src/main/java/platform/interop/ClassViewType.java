package platform.interop;

public class ClassViewType {

    public static final byte PANEL = 1;
    public static final byte GRID = 2;
    public static final byte HIDE = 4;

    public static byte switchView(byte initClassView) {

        if (initClassView == GRID)
            return PANEL;
        else
            return GRID;
    }

    public static byte getByte(String action) {

        if (action.equals("grid"))
            return GRID;
        else if (action.equals("panel"))
            return PANEL;
        else
            return HIDE;
    }
}
