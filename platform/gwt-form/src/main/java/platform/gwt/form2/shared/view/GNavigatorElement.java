package platform.gwt.form2.shared.view;

import platform.gwt.form2.shared.view.window.GNavigatorWindow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GNavigatorElement implements Serializable {
    public String sid;
    public String caption;
    public String icon;
    public boolean isForm;

    public boolean hasChildren;
    public ArrayList<GNavigatorElement> children;
    public Set<GNavigatorElement> parents = new HashSet<GNavigatorElement>();

    public GNavigatorWindow window;

    public boolean containsParent(Set<GNavigatorElement> set) {
        for (GNavigatorElement element : parents) {
            if (set.contains(element)) return true;
        }
        return false;
    }
}
