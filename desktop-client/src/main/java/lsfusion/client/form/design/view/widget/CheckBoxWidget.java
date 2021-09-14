package lsfusion.client.form.design.view.widget;

import javax.swing.*;

public class CheckBoxWidget extends JCheckBox implements Widget {

    public CheckBoxWidget() {
        Widget.addMouseListeners(this);
    }

    public CheckBoxWidget(String text) {
        super(text);

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
