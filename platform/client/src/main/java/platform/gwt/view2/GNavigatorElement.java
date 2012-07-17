package platform.gwt.view2;

import java.io.Serializable;
import java.util.ArrayList;

public class GNavigatorElement implements Serializable {
    public String sid;
    public String caption;
    public String icon;
    public boolean isForm;

    public ArrayList<GNavigatorElement> children;
}
