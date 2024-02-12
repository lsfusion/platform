package lsfusion.gwt.client.form.filter;

import lsfusion.gwt.client.form.event.GBindingMode;
import lsfusion.gwt.client.form.event.GKeyInputEvent;

import java.io.Serializable;
import java.util.Map;

public class GRegularFilter implements Serializable {
    public int ID;

    public String caption;
    public GKeyInputEvent keyEvent;
    public Map<String, GBindingMode> bindingModes;
    public Integer priority;
    public boolean showKey;

    public GRegularFilter() {
        ID = -1;
    }

    public String getFullCaption() {
        String fullCaption = caption;
        if (showKey && keyEvent != null) {
            fullCaption += " (" + keyEvent.keyStroke + ")";
        }
        return fullCaption;
    }
}
