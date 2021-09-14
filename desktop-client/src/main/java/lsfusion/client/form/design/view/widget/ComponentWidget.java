package lsfusion.client.form.design.view.widget;

import javax.swing.*;

public class ComponentWidget extends JComponent implements Widget {

    public ComponentWidget() {
        Widget.addMouseListeners(this);
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
