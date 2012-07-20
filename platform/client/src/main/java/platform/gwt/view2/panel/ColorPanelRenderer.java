package platform.gwt.view2.panel;

import platform.gwt.view2.GPropertyDraw;

public class ColorPanelRenderer extends TextBoxPanelRenderer {

    public ColorPanelRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public void setValue(Object value) {
        super.setValue(value);
        textBox.getElement().getStyle().setBackgroundColor(value.toString());
    }
}
