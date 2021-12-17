package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.Widget;

public interface ResizableMainPanel {
    void setSizedMain(Widget main, boolean autoSize);
    Widget getPanelWidget();
}