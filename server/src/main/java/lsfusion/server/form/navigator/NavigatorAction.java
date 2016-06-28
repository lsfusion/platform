package lsfusion.server.form.navigator;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.ActionProperty;

public class NavigatorAction<T extends BusinessLogics<T>> extends NavigatorElement<T> {
    private ActionProperty property;

    public NavigatorAction(NavigatorElement<T> parent, String canonicalName, String caption, String icon, Version version) {
        super(parent, canonicalName, caption, icon, version);
        setImage(icon != null ? icon : "/images/action.png", icon != null ? null : DefaultIcon.ACTION);
    }

    @Override
    protected String getAnonymousSIDPrefix() {
        return ACTION_ANONYMOUS_SID_PREFIX;
    }

    @Override
    public byte getTypeID() {
        return 2;
    }

    public void setProperty(ActionProperty property) {
        this.property = property;
    }

    public ActionProperty getProperty() {
        return property;
    }
}