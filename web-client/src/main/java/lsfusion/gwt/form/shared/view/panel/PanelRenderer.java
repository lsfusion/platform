package lsfusion.gwt.form.shared.view.panel;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;

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

    void addedToFlexPanel(FlexPanel parent, GFlexAlignment alignment, double flex);
}
