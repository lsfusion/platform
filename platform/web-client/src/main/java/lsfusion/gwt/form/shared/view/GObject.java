package lsfusion.gwt.form.shared.view;

import java.io.Serializable;

public class GObject implements Serializable {
    public GGroupObject groupObject;
    public String caption;
    public int ID;
    public String sID;

    public String getCaption() {
        return caption;
    }

    @Override
    public String toString() {
        return getCaption();
    }
}
