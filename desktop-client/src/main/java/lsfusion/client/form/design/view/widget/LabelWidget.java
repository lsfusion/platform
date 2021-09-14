package lsfusion.client.form.design.view.widget;

import javax.swing.*;

public class LabelWidget extends JLabel implements Widget {

    public LabelWidget(String text) {
        super(text);

        Widget.addMouseListeners(this);
    }

    public LabelWidget() {
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
