package platform.server.logics.session;

import platform.server.logics.classes.DataClass;

public abstract class ChangeValue {
    public DataClass Class;

    ChangeValue(DataClass iClass) {
        Class = iClass;
    }
}
