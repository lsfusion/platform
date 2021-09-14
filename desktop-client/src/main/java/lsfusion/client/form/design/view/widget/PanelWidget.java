package lsfusion.client.form.design.view.widget;

import javax.swing.*;
import java.awt.*;

public class PanelWidget extends JPanel implements Widget {

    public PanelWidget() {
        Widget.addMouseListeners(this);
    }

    public PanelWidget(LayoutManager layout) {
        super(layout);

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
