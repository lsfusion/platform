package platform.server.classes;

public class CustomObjectClass extends ConcreteCustomClass {

    public CustomObjectClass(String sID, String caption, CustomClass... parents) {
        super(sID, caption, parents);
    }

    public Integer stat = 100000;
    public int getCount() {
        return stat;
    }
}
