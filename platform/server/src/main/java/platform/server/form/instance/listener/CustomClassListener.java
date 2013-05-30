package platform.server.form.instance.listener;

import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;

public interface CustomClassListener {

    Integer getObject(CustomClass cls);
    void objectChanged(ConcreteCustomClass cls, int objectID);
}
