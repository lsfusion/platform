package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.base.BaseStaticImage;
import lsfusion.gwt.client.form.event.GBindingMode;
import lsfusion.gwt.client.form.event.GKeyStroke;

import java.io.Serializable;
import java.util.ArrayList;

public class GInputListAction implements Serializable {
    public BaseStaticImage action;
    public String id;
    public GAsyncEventExec asyncExec;
    public GKeyStroke keyStroke;
    public GBindingMode editingBindingMode;
    public ArrayList<GQuickAccess> quickAccessList;
    public int index;

    @SuppressWarnings("unused")
    public GInputListAction() {
    }

    public GInputListAction(BaseStaticImage action, String id, GAsyncEventExec asyncExec, GKeyStroke keyStroke, GBindingMode editingBindingMode, ArrayList<GQuickAccess> quickAccessList, int index) {
        this.action = action;
        this.id = id;
        this.asyncExec = asyncExec;
        this.keyStroke = keyStroke;
        this.editingBindingMode = editingBindingMode;
        this.quickAccessList = quickAccessList;
        this.index = index;
    }
}