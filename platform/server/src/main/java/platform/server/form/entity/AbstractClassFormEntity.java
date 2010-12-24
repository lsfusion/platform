package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.logics.BusinessLogics;

public abstract class AbstractClassFormEntity <T extends BusinessLogics<T>> extends FormEntity<T> {
    protected final T BL;
    protected final CustomClass cls;

    protected AbstractClassFormEntity(T BL, CustomClass cls, int ID, String caption) {
        super(ID, caption);
        this.BL = BL;
        this.cls = cls;
    }

    public AbstractClassFormEntity createCopy() {
        AbstractClassFormEntity form = copy();
        form.setID(form.ID + 10000);
        return form;
    }

    public abstract ObjectEntity getObject();

    protected abstract AbstractClassFormEntity copy();
}
