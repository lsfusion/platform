package lsfusion.client.form.layout;

import lsfusion.interop.form.layout.Alignment;
import lsfusion.interop.form.layout.FlexLayout;

import javax.swing.*;
import java.awt.*;

import static lsfusion.client.SwingUtils.overrideSize;

// выполняет роль FlexPanel в web
public class JComponentPanel extends JPanel {

    public JComponentPanel(boolean vertical, Alignment alignment) {
        super(null);
        setLayout(new FlexLayout(this, vertical, alignment));
    }
    public JComponentPanel() {
        this(new BorderLayout());
    }
    public JComponentPanel(LayoutManager layout) {
        super(layout);
    }

    private Dimension componentSize;

    public void setComponentSize(Dimension componentSize) {
        this.componentSize = componentSize;
    }

    @Override
    public Dimension getPreferredSize() {
        return overrideSize(super.getPreferredSize(), componentSize);
    }

    public Dimension getMaxPreferredSize() {
        return getPreferredSize();
    }
}
