package lsfusion.server.profiler;

import lsfusion.server.form.entity.FormEntity;

public class RMICallProfileObject extends ProfileObject {
    public RMICallProfileObject(Object profileObject, String methodName) {
        super(methodName, profileObject);
    }

    @Override
    public String getProfileString() {
        return "RMI call: " + super.getProfileString();
    }
}
