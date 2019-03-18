package lsfusion.client.form.user.queries;

import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

abstract class FilterValueView extends JPanel {

    protected final FilterValueListener listener;

    FilterValueView(FilterValueListener listener) {
        assert listener != null;
        this.listener = listener;
        setLayout(new BorderLayout());
    }

    public void propertyChanged(ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        //do nothing by default
    }

    public void startEditing(KeyEvent initFilterKeyEvent) {
        requestFocusInWindow();
    }
}
