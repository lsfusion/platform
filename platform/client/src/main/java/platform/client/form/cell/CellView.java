package platform.client.form.cell;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public interface CellView {

    JComponent getComponent();
    void addListener(CellViewListener listener);
    void setValue(Object ivalue);
    void startEditing(KeyEvent e);
    void setCaption(String caption);
    void setHighlight(Object highlight, Color highlightColor);
}