package lsfusion.server.form.instance.listener;

import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.CustomClass;

public interface CustomClassListener {

    Long getObject(CustomClass cls);
    void objectChanged(ConcreteCustomClass cls, long objectID);
    
    boolean isDeactivated(); // потом надо будет переделать на отдельный интерфейс
}
