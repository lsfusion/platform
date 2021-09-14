package lsfusion.client.form.design.view.widget;

import javax.swing.*;

public class TextFieldWidget extends JTextField implements Widget {

    public TextFieldWidget() {
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
