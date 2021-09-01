package lsfusion.gwt.client.form.object;

import java.io.Serializable;

public class GObject implements Serializable {
    public GGroupObject groupObject;
    public String caption;
    public int ID;
    public String sID;

    public GObject() {
    }

    public GObject(GGroupObject groupObject, String caption, int ID, String sID) {
        this.groupObject = groupObject;
        this.caption = caption;
        this.ID = ID;
        this.sID = sID;
    }


    public String getCaption() {
        return caption;
    }

    @Override
    public String toString() {
        return getCaption();
    }
}
