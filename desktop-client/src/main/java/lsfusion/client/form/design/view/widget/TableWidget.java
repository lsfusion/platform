package lsfusion.client.form.design.view.widget;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class TableWidget extends JTable implements Widget {

    public TableWidget() {
        Widget.addMouseListeners(this);
    }

    public TableWidget(TableModel dm) {
        super(dm);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focusChanged(e, true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                focusChanged(e, false);
            }
        });

    }

    public void focusChanged(FocusEvent e, boolean focused) {
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
