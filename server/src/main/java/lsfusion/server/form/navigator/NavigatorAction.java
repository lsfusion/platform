package lsfusion.server.form.navigator;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.ActionProperty;

import java.io.DataOutputStream;
import java.io.IOException;

public class NavigatorAction<T extends BusinessLogics<T>> extends NavigatorElement<T> {
    private ActionProperty property;

    public NavigatorAction(String sID, String caption) {
        this(null, sID, caption);
    }

    public NavigatorAction(NavigatorElement<T> parent, String sID, String caption) {
        this(parent, sID, caption, null);
    }

    public NavigatorAction(NavigatorElement<T> parent, String sID, String caption, String icon) {
        super(parent, sID, caption, icon);
        setImage(icon != null ? icon : "/images/action.png");
    }

    @Override
    public byte getTypeID() {
        return 2;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
    }

    public void setProperty(ActionProperty property) {
        this.property = property;
    }

    public ActionProperty getProperty() {
        return property;
    }
}