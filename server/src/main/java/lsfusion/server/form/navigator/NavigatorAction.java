package lsfusion.server.form.navigator;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.ActionProperty;

import java.io.DataOutputStream;
import java.io.IOException;

public class NavigatorAction<T extends BusinessLogics<T>> extends NavigatorElement<T> {
    private ActionProperty property;

    public NavigatorAction(String sID, String caption, Version version) {
        this(null, sID, caption, version);
    }

    public NavigatorAction(NavigatorElement<T> parent, String sID, String caption, Version version) {
        this(parent, sID, caption, null, version);
    }

    public NavigatorAction(NavigatorElement<T> parent, String sID, String caption, String icon, Version version) {
        super(parent, sID, caption, icon, version);
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