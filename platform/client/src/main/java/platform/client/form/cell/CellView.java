package platform.client.form.cell;

import javax.swing.*;
import java.awt.event.KeyEvent;

public interface CellView {

    JComponent getComponent();
    void addListener(CellViewListener listener);
    void setValue(Object ivalue);
    void startEditing(KeyEvent e);
    void keyUpdated();
}