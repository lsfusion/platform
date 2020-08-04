package lsfusion.client.form.property.panel.view;

import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;

import javax.swing.*;
import java.awt.*;

public interface PanelView {
    JComponent getComponent();
    JComponent getFocusComponent();
    void setValue(Object ivalue);
    void setReadOnly(boolean readOnly);
    boolean forceEdit();
    boolean isShowing();
    void setCaption(String caption);
    void setToolTip(String caption);
    void setBackgroundColor(Color background);
    void setForegroundColor(Color foreground);
    void setImage(Object image);

    Icon getIcon();
    void setIcon(Icon icon);
    
    EditPropertyDispatcher getEditPropertyDispatcher();
}