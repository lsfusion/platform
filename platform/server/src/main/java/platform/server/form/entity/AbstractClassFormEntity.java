package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.logics.BusinessLogics;

public abstract class AbstractClassFormEntity <T extends BusinessLogics<T>> extends FormEntity<T> {
    protected final T BL;
    protected final CustomClass cls;
    protected int copies = 0;

    protected AbstractClassFormEntity(T BL, CustomClass cls, String sID, String caption) {
        super(sID, caption);
        this.BL = BL;
        this.cls = cls;
    }

    public AbstractClassFormEntity createCopy() {
        return copy();
    }

    public abstract ObjectEntity getObject();

    protected abstract AbstractClassFormEntity copy();
}
