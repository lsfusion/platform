package platform.gwt.view2.classes;

import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.logics.FormLogicsProvider;
import platform.gwt.view2.panel.LongPanelRenderer;
import platform.gwt.view2.panel.PanelRenderer;

public class GLongType extends GIntegralType {
    public static GType instance = new GLongType();

    @Override
    public Object parseString(String strValue) {
        return Long.parseLong(strValue);
    }

    @Override
    public PanelRenderer createPanelRenderer(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new LongPanelRenderer(property);
    }
}
