package platform.client.form.cell;

import javax.swing.*;

public interface CellView {

    JComponent getComponent();
    void addListener(CellViewListener listener);
    void setValue(Object ivalue);
    void startEditing();
}