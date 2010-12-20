package platform.server.form.entity;

import platform.server.logics.BusinessLogics;

public abstract class AbstractClassFormEntity <T extends BusinessLogics<T>> extends FormEntity<T> {
    protected AbstractClassFormEntity(int ID, String caption) {
        super(ID, caption);
    }

    public abstract ObjectEntity getObject();
}
