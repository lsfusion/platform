package lsfusion.gwt.client.navigator;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.base.ImageHolder;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public abstract class GNavigatorElement implements Serializable {
    public String canonicalName;
    public String caption;
    public String creationPath;
    public ImageHolder image;

    public ArrayList<GNavigatorElement> children;
    public HashSet<GNavigatorElement> parents = new HashSet<>();

    public GNavigatorWindow window;

    public boolean containsParent(Set<GNavigatorElement> set) {
        for (GNavigatorElement parent : parents) {
            if (set.contains(parent)) return true;
        }
        return false;
    }
    
    public ImageDescription getImage() {
        return image != null ? image.getImage() : null;
    }

    public String getTooltipText() {
        return MainFrame.showDetailedInfo ?
                GwtSharedUtils.stringFormat("<html><b>%s</b><hr><b>sID:</b> %s<br><b>" + ClientMessages.Instance.get().tooltipPath() 
                        + ":</b> %s</html>", caption, canonicalName, creationPath) : caption;
    }
}
