package platform.gwt.view2.classes;

public class GDoubleType extends GIntegralType {
    public static GType instance = new GDoubleType();

    @Override
    public Object parseString(String strValue) {
        return Double.parseDouble(strValue);
    }
}
