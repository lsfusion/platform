package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;

public abstract class BaseClassFormEntity <T extends BusinessLogics<T>> extends AbstractClassFormEntity<T> {

    protected final BaseLogicsModule<T> LM;
    protected final CustomClass cls;

    protected BaseClassFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(sID, caption);
        this.LM = LM;
        this.cls = cls;
    }

}
