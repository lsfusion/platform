package lsfusion.server.profiler;

import lsfusion.server.form.entity.FormEntity;

public class RMICallProfileObject extends ProfileObject {
    public RMICallProfileObject(FormEntity form, String methodName) {
        super(methodName, form);
    }

    @Override
    public String getProfileString() {
        return "RMI call: " + super.getProfileString();
    }
}
