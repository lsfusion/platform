package lsfusion.server.physics.admin.profiler;

import lsfusion.server.logics.action.ActionProperty;

public class ActionProfileObject extends ProfileObject {
    public ActionProfileObject(ActionProperty property) {
        super(property);
    }

    @Override
    public String getProfileString() {
        return "Action property: " + super.getProfileString();
    }
}
