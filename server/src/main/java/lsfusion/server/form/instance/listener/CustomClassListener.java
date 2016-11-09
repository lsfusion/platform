package lsfusion.server.form.instance.listener;

import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.CustomClass;

public interface CustomClassListener {

    Integer getObject(CustomClass cls);
    void objectChanged(ConcreteCustomClass cls, int objectID);
    
    boolean isClosed(); // потом надо будет переделать на отдельный интерфейс 
}
