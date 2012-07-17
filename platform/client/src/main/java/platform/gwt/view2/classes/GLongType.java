package platform.gwt.view2.classes;

public class GLongType extends GIntegralType {
    public static GType instance = new GLongType();

    @Override
    public Object parseString(String strValue) {
        return Long.parseLong(strValue);
    }
}
