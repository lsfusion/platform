package lsfusion.gwt.client.navigator;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.base.ImageHolder;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.form.property.async.GAsyncExec;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static lsfusion.gwt.client.base.GwtClientUtils.createTooltipHorizontalSeparator;

public abstract class GNavigatorElement implements Serializable {
    public String canonicalName;
    public String caption;
    public String creationPath;
    public String path;
    public ImageHolder image;

    public GAsyncExec asyncExec;

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
        String command = TooltipManager.getCommand(creationPath, path);
        return MainFrame.showDetailedInfo ?
                GwtSharedUtils.stringFormat("<html><b>%s</b>" +
                        createTooltipHorizontalSeparator() +
                        "<b>sID:</b> %s<br>" +
                        "<b>" + ClientMessages.Instance.get().tooltipPath() + ":</b> %s</br>" +
                        (!command.isEmpty() ? "<a href=\"lsfusion-protocol://%s\" target=\"_blank\">" + ClientMessages.Instance.get().showInEditor() + "</a>" : "") +
                        "</html>", caption, canonicalName, creationPath, command) : caption;
    }
}
