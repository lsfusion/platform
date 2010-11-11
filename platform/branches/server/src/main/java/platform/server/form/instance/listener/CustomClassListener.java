package platform.server.form.instance.listener;

import platform.server.classes.ConcreteCustomClass;

public interface CustomClassListener {

    void objectChanged(ConcreteCustomClass cls, int objectID);
}
