package lsfusion.client.form.property.panel.view;

import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;

import javax.swing.*;
import java.awt.*;

public interface PanelView {
    Widget getWidget();
    JComponent getFocusComponent();
    void setValue(Object ivalue);
    void setReadOnly(boolean readOnly);
    boolean forceEdit();
    boolean isShowing();
    void setCaption(String caption);
    void setTooltip(String caption);
    void setBackgroundColor(Color background);
    void setForegroundColor(Color foreground);
    void setImage(Image image);

    Icon getIcon();
    void setIcon(Icon icon);
    
    EditPropertyDispatcher getEditPropertyDispatcher();
}