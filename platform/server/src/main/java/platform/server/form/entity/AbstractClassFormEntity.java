package platform.server.form.entity;

import platform.server.logics.BusinessLogics;

public abstract class AbstractClassFormEntity <T extends BusinessLogics<T>> extends FormEntity<T> {
    protected AbstractClassFormEntity(String sID, String caption) {
        super(sID, caption);
    }

    public abstract ObjectEntity getObject();
}
