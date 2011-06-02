package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.form.navigator.NavigatorElement;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;

public abstract class AbstractClassFormEntity <T extends BusinessLogics<T>> extends FormEntity<T> {
    protected int copies = 0;

    protected AbstractClassFormEntity(String sID, String caption) {
        super(sID, caption);
    }

    public AbstractClassFormEntity createCopy() {
        return copy();
    }

    public abstract ObjectEntity getObject();

    protected abstract AbstractClassFormEntity copy();
}
