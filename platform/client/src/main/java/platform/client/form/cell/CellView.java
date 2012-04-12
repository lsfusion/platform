package platform.client.form.cell;

import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public interface CellView {

    JComponent getComponent();
    void addListener(CellViewListener listener);
    void setValue(Object ivalue);
    void startEditing(KeyEvent e);
    void setCaption(String caption);
    void setToolTip(String caption);
    void setBackground(Object background);
    void changeViewType(ClassViewType type);
}