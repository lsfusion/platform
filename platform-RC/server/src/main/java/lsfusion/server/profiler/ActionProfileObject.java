package lsfusion.server.profiler;

import lsfusion.server.logics.property.ActionProperty;

public class ActionProfileObject extends ProfileObject {
    public ActionProfileObject(ActionProperty property) {
        super(property);
    }

    @Override
    public String getProfileString() {
        return "Action property: " + super.getProfileString();
    }
}
