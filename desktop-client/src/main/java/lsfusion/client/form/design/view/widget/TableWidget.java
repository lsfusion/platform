package lsfusion.client.form.design.view.widget;

import javax.swing.*;
import javax.swing.table.TableModel;

public class TableWidget extends JTable implements Widget {

    public TableWidget() {
        Widget.addMouseListeners(this);
    }

    public TableWidget(TableModel dm) {
        super(dm);
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public String toString() {
        return Widget.toString(this, super.toString());
    }
}
