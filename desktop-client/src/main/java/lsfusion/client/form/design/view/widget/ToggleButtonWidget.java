package lsfusion.client.form.design.view.widget;

import javax.swing.*;

public class ToggleButtonWidget extends JToggleButton implements Widget {

    public ToggleButtonWidget(String text) {
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
