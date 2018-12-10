package lsfusion.gwt.shared.view;

import lsfusion.gwt.shared.base.GwtSharedUtils;
import lsfusion.gwt.client.form.MainFrame;
import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.shared.view.window.GNavigatorWindow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public abstract class GNavigatorElement implements Serializable {
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
                GwtSharedUtils.stringFormat("<html><b>%s</b><hr><b>sID:</b> %s<br><b>" + MainFrameMessages.Instance.get().tooltipPath() 
                        + ":</b> %s</html>", caption, canonicalName, creationPath) : caption;
    }
}
