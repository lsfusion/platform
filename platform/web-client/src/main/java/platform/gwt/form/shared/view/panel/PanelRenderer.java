package platform.gwt.form.shared.view.panel;

import com.google.gwt.user.client.ui.Widget;

import java.io.Serializable;

public interface PanelRenderer extends Serializable {
    Widget getComponent();
    void setValue(Object value);
    void setCaption(String caption);
    void setDefaultIcon();
    void setIcon(String iconPath);

    void updateCellBackgroundValue(Object value);
    void updateCellForegroundValue(Object value);

    void focus();
}
