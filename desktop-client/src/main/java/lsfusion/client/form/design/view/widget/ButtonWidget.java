package lsfusion.client.form.design.view.widget;

import javax.swing.*;

public class ButtonWidget extends JButton implements Widget {

    public ButtonWidget() {
        Widget.addMouseListeners(this);
    }

    public ButtonWidget(String text, Icon icon) {
        super(text, icon);

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
