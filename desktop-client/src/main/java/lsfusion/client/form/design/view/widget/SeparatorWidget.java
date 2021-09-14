package lsfusion.client.form.design.view.widget;

import javax.swing.*;

public class SeparatorWidget extends JSeparator implements Widget {

    public SeparatorWidget(int orientation) {
        super(orientation);

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
