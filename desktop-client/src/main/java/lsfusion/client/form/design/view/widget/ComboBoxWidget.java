package lsfusion.client.form.design.view.widget;

import javax.swing.*;

public class ComboBoxWidget extends JComboBox implements Widget {

    public ComboBoxWidget() {
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
