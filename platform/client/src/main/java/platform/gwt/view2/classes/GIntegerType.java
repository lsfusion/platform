package platform.gwt.view2.classes;

public class GIntegerType extends GIntegralType {
    public static GType instance = new GIntegerType();

    @Override
    public Object parseString(String strValue) {
        return Integer.parseInt(strValue);
    }
}
