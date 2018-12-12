package lsfusion.gwt.client.form.ui.panel;

import com.google.gwt.user.client.ui.Widget;

public interface PanelRenderer {
    Widget getComponent();
    void setValue(Object value);
    void setReadOnly(boolean readOnly);
    void setCaption(String caption);
    void setDefaultIcon();
    void setIcon(String iconPath);

    void updateCellBackgroundValue(Object value);
    void updateCellForegroundValue(Object value);

    void focus();
}
