package platform.server.logics.session;

import platform.server.logics.classes.DataClass;

public class ChangeObjectValue extends ChangeValue {
    public Object Value;

    public ChangeObjectValue(DataClass iClass, Object iValue) {
        super(iClass);
        Value = iValue;
    }
}
