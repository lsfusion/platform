package lsfusion.gwt.client.navigator;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.jsni.HasNativeSID;
import lsfusion.gwt.client.form.property.async.GAsyncExec;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

import static lsfusion.gwt.client.base.GwtClientUtils.createTooltipHorizontalSeparator;

public abstract class GNavigatorElement implements Serializable, HasNativeSID {
    public String canonicalName;
    public String caption;
    public String creationPath;
    public String path;
    public BaseImage image;
    public String elementClass;

    public GAsyncExec asyncExec;

    public ArrayList<GNavigatorElement> children;
    public GNavigatorElement parent;

    public GNavigatorWindow window;
    public boolean parentWindow;

    public GNavigatorWindow getDrawWindow() {
        if(parentWindow)
            return window;
        if(parent != null) {
            if(parent.window != null)
                return parent.window;
            return parent.getDrawWindow();
        }
        return null;
    }

    public GNavigatorElement findChild(GNavigatorElement element) {
        if (element == null) {
            return null;
        }
        if (element == this) {
            return this;
        }

        for (GNavigatorElement child : children) {
            GNavigatorElement found = child.findChild(element);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public String getTooltipText() {
        return MainFrame.showDetailedInfo ?
                GwtSharedUtils.stringFormat("<html><b>%s</b>" +
                        createTooltipHorizontalSeparator() +
                        "<b>sID:</b> %s<br><b>" + ClientMessages.Instance.get().tooltipPath() +
                        ":</b> %s<a class='lsf-tooltip-path'></a> &ensp; <a class='lsf-tooltip-help'></a></html>",
                        caption, canonicalName, creationPath) : caption;
    }

    @Override
    public String getNativeSID() {
        return canonicalName;
    }

    public boolean isFolder() {
        return false;
    }
}
