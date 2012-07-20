package platform.gwt.view2.panel;

import com.google.gwt.user.client.ui.Widget;
import platform.gwt.view2.GPropertyDraw;

public class ActionPanelRenderer implements PanelRenderer {

    private final ImageButton button;

    public ActionPanelRenderer(GPropertyDraw property) {
        button = new ImageButton(property.caption, property.iconPath);
    }

    @Override
    public Widget getComponent() {
        return button;
    }

    @Override
    public void setValue(Object value) {
        button.setEnabled(value != null && (Boolean)value);
    }

    @Override
    public void setTitle(String title) {
        button.setText(title);
    }

    @Override
    public void updateCellBackgroundValue(Object value) {
        //todo:
    }

    @Override
    public void updateCellForegroundValue(Object value) {
        //todo:
    }
}
