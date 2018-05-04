package lsfusion.client.form.queries;

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

    public void propertyChanged(ClientPropertyDraw property) {
        //do nothing by default
    }

    public void startEditing(KeyEvent initFilterKeyEvent) {
        requestFocusInWindow();
    }
}
