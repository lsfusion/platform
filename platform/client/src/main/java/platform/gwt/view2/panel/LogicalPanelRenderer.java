package platform.gwt.view2.panel;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.view2.GPropertyDraw;

public class LogicalPanelRenderer implements PanelRenderer {

    private final CheckBox checkBox;

    public LogicalPanelRenderer(GPropertyDraw property) {
        checkBox = new CheckBox(property.getCaptionOrEmpty());
    }

    @Override
    public Widget getComponent() {
        return checkBox;
    }

    @Override
    public void setValue(Object value) {
        checkBox.setValue(value instanceof Boolean ? (Boolean) value : value != null);
    }

    @Override
    public void setTitle(String title) {
        checkBox.setText(title);
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
