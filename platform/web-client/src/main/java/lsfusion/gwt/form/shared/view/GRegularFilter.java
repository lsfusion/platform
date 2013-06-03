package lsfusion.gwt.form.shared.view;

import java.io.Serializable;

public class GRegularFilter implements Serializable {
    public int ID;

    public String caption;
    public GKeyStroke key;
    public boolean showKey;

    public GRegularFilter() {
        ID = -1;
    }

    public String getFullCaption() {
        String fullCaption = caption;
        if (showKey && key != null) {
            fullCaption += " (" + key + ")";
        }
        return fullCaption;
    }
}
