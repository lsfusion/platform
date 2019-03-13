package lsfusion.server.logics.form.interactive.listener;

import lsfusion.server.logics.classes.ConcreteCustomClass;
import lsfusion.server.logics.classes.CustomClass;

public interface CustomClassListener {

    Long getObject(CustomClass cls);
    void objectChanged(ConcreteCustomClass cls, long objectID);
    
    boolean isDeactivated(); // потом надо будет переделать на отдельный интерфейс
}
