package platform.server.classes;

import platform.server.logics.ServerResourceBundle;

public class CustomObjectClass extends ConcreteCustomClass {

    public CustomObjectClass(CustomClass parent) {
        super("CustomObjectClass", ServerResourceBundle.getString("classes.object.class"), parent);
    }
}
