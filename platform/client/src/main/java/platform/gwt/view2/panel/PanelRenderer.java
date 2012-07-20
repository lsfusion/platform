package platform.gwt.view2.panel;

import com.google.gwt.user.client.ui.Widget;

import java.io.Serializable;

public interface PanelRenderer extends Serializable {
    Widget getComponent();
    void setValue(Object value);
    void setTitle(String title);

    void updateCellBackgroundValue(Object value);
    void updateCellForegroundValue(Object value);
}
