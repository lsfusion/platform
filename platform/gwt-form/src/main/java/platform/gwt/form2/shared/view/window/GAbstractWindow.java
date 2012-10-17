package platform.gwt.form2.shared.view.window;

import java.io.Serializable;

public class GAbstractWindow implements Serializable {
    public String caption;
    public int position;

    public int x;
    public int y;
    public int width;
    public int height;

    public String borderConstraint;

    public boolean titleShown;
    public boolean visible;

    public boolean initialSizeSet = false;
}
