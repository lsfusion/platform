package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.Widget;

public interface ResizableMainPanel {
    void setFillMain(Widget main);
    void setPercentMain(Widget main);
    Widget getPanelWidget();
}