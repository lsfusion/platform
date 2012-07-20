package platform.gwt.view2.classes;

import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.logics.FormLogicsProvider;
import platform.gwt.view2.panel.PanelRenderer;
import platform.gwt.view2.panel.TextPanelRenderer;

public class GTextType extends GDataType {
    public static GType instance = new GTextType();

    @Override
    public PanelRenderer createPanelRenderer(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new TextPanelRenderer(property);
    }
}
