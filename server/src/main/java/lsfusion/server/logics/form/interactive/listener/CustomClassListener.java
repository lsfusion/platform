package lsfusion.server.logics.form.interactive.listener;

import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;

public interface CustomClassListener {

    Long getObject(CustomClass cls, FormEntity form, GroupObjectEntity groupObject);
    void objectChanged(ConcreteCustomClass cls, FormEntity form, GroupObjectEntity groupObject, long objectID);
    
    boolean isDeactivated(); // temporary - later some separate interface should be used

    boolean isUseBootstrap(); // temporary - later some separate interface should be used
    boolean isNative(); // temporary - later some separate interface should be used
    boolean isMobile(); // temporary - later some separate interface should be used
}
