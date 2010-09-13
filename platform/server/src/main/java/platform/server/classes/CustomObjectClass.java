package platform.server.classes;

import platform.server.logics.BusinessLogics;

public class CustomObjectClass extends ConcreteCustomClass {

    public CustomObjectClass(CustomClass parent) {
        super(Integer.MAX_VALUE-5, "Класс объекта", parent);
    }
}
