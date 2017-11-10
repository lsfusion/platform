package lsfusion.gwt.form.shared.view;

import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.client.MainFrame;
import lsfusion.gwt.form.shared.view.window.GNavigatorWindow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GNavigatorElement implements Serializable {
    public String sid;
    public String canonicalName;
    public String caption;
    public String creationPath;
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

    public String getTooltipText() {
        return MainFrame.configurationAccessAllowed ?
                GwtSharedUtils.stringFormat("<html><b>%s</b><hr><b>sID:</b> %s<br><b>Путь:</b> %s</html>", caption, sid, creationPath) : caption;
    }
}
