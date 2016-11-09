package lsfusion.gwt.form.shared.view;

import lsfusion.gwt.form.shared.view.window.GNavigatorWindow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GNavigatorElement implements Serializable {
    public String sid;
    public String canonicalName;
    public String caption;
    public ImageDescription icon;

    public ArrayList<GNavigatorElement> children;
    public HashSet<GNavigatorElement> parents = new HashSet<>();

    public GNavigatorWindow window;

    public boolean containsParent(Set<GNavigatorElement> set) {
        for (GNavigatorElement parent : parents) {
            if (set.contains(parent)) return true;
        }
        return false;
    }
}
