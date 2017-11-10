package lsfusion.client.form.cell;

import lsfusion.client.form.dispatch.EditPropertyDispatcher;

import javax.swing.*;
import java.awt.*;

public interface PanelView {
    JComponent getComponent();
    void setValue(Object ivalue);
    void setReadOnly(boolean readOnly);
    boolean forceEdit();
    void setCaption(String caption);
    void setToolTip(String caption);
    void setBackgroundColor(Color background);
    void setForegroundColor(Color foreground);

    Icon getIcon();
    void setIcon(Icon icon);
    
    void setLabelWidth(int width);
    EditPropertyDispatcher getEditPropertyDispatcher();
}