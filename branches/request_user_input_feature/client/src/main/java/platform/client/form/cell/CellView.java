package platform.client.form.cell;

import platform.interop.ClassViewType;

import javax.swing.*;

public interface CellView {
    JComponent getComponent();
    void setListener(CellViewListener listener);
    void setValue(Object ivalue);
    void forceEdit();
    void setCaption(String caption);
    void setToolTip(String caption);
    void setBackground(Object background);
    void setForeground(Object foreground);
    void changeViewType(ClassViewType type);
}