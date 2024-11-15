package lsfusion.gwt.client.navigator;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.jsni.HasNativeSID;
import lsfusion.gwt.client.form.event.GInputBindingEvent;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GMouseInputEvent;
import lsfusion.gwt.client.form.property.async.GAsyncExec;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;
import java.util.ArrayList;

import static lsfusion.gwt.client.base.GwtClientUtils.createTooltipHorizontalSeparator;
import static lsfusion.gwt.client.base.GwtClientUtils.getEventCaption;

public abstract class GNavigatorElement implements Serializable, HasNativeSID {
    public String canonicalName;
    public String caption;

    public ArrayList<GInputBindingEvent> bindingEvents = new ArrayList<>();
    public boolean showChangeKey;
    public boolean showChangeMouse;

    public String creationPath;
    public String path;
    public BaseImage image;
    public String elementClass;
    public boolean hide;

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

    public String getCaption() {
        String eventCaption = getEventCaption(showChangeKey && hasKeyBinding() ? getKeyBindingText() : null,
                showChangeMouse && hasMouseBinding() ? getMouseBindingText() : null);
        return caption + (eventCaption != null ? " (" + eventCaption + ")" : "");
    }

    private boolean hasKeyBinding() {
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GKeyInputEvent)
                return true;
        return false;
    }
    private String getKeyBindingText() {
        assert hasKeyBinding();
        String result = "";
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GKeyInputEvent) {
                result = (result.isEmpty() ? "" : result + ",") + ((GKeyInputEvent) bindingEvent.inputEvent).keyStroke;
            }
        return result;
    }

    private boolean hasMouseBinding() {
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GMouseInputEvent)
                return true;
        return false;
    }
    private String getMouseBindingText() {
        assert hasMouseBinding();
        String result = "";
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GMouseInputEvent) {
                result = (result.isEmpty() ? "" : result + ",") + ((GMouseInputEvent) bindingEvent.inputEvent).mouseEvent;
            }
        return result;
    }

    public String getTooltipText() {
        return MainFrame.showDetailedInfo ?
                GwtSharedUtils.stringFormat("<html>%s" +
                        "<b>sID:</b> %s<br><b>" + ClientMessages.Instance.get().tooltipPath() +
                        ":</b> %s<a class='lsf-tooltip-path'></a> &ensp; <a class='lsf-tooltip-help'></a></html>",
                        (caption != null ? ("<b>" + getCaption() + "</b>" + createTooltipHorizontalSeparator()) : ""),
                        canonicalName, creationPath) : getCaption();
    }

    @Override
    public String getNativeSID() {
        return canonicalName;
    }

    public boolean isFolder() {
        return false;
    }
}
