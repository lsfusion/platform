package platform.gwt.view2.classes;

import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.logics.FormLogicsProvider;
import platform.gwt.view2.panel.DoublePanelRenderer;
import platform.gwt.view2.panel.PanelRenderer;

public class GDoubleType extends GIntegralType {
    public static GType instance = new GDoubleType();

    @Override
    public Object parseString(String strValue) {
        return Double.parseDouble(strValue);
    }

    @Override
    public PanelRenderer createPanelRenderer(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new DoublePanelRenderer(property);
    }
}
