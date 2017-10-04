package lsfusion.server.form.navigator;

import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ActionProperty;

public class NavigatorAction extends NavigatorElement {
    private final ActionProperty action;

    public NavigatorAction(ActionProperty action, String canonicalName, LocalizedString caption) {
        super(canonicalName, caption);
        
        this.action = action;
        setImage("/images/action.png", DefaultIcon.ACTION);
    }

    @Override
    public boolean isLeafElement() {
        return true;
    }

    @Override
    public byte getTypeID() {
        return 2;
    }

    public ActionProperty getAction() {
        return action;
    }
}