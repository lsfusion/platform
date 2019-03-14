package lsfusion.server.physics.admin.profiler;

import lsfusion.server.logics.action.Action;

public class ActionProfileObject extends ProfileObject {
    public ActionProfileObject(Action property) {
        super(property);
    }

    @Override
    public String getProfileString() {
        return "Action property: " + super.getProfileString();
    }
}
