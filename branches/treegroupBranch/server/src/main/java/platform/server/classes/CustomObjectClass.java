package platform.server.classes;

public class CustomObjectClass extends ConcreteCustomClass {

    public CustomObjectClass(CustomClass parent) {
        super(Integer.MAX_VALUE-5, "Класс объекта", parent);
    }
}
