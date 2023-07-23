package lsfusion.server.logics.form.interactive.listener;

import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;

public interface CustomClassListener {

    Long getObject(CustomClass cls);
    void objectChanged(ConcreteCustomClass cls, long objectID);
    
    boolean isDeactivated(); // temporary - later some separate interface should be used

    boolean isUseBootstrap(); // temporary - later some separate interface should be used
    boolean isNative(); // temporary - later some separate interface should be used
}
