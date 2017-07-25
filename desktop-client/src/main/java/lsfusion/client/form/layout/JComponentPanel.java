package lsfusion.client.form.layout;

import lsfusion.client.logics.ClientComponent;
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

    private Dimension componentMinimumSize;
    private Dimension componentMaximumSize;
    private Dimension componentPreferredSize;

    public void setComponentMinimumSize(Dimension componentMinimumSize) {
        this.componentMinimumSize = componentMinimumSize;
    }

    public void setComponentMaximumSize(Dimension componentMaximumSize) {
        this.componentMaximumSize = componentMaximumSize;
    }

    public void setComponentPreferredSize(Dimension componentPreferredSize) {
        this.componentPreferredSize = componentPreferredSize;
    }

    @Override
    public Dimension getMinimumSize() {
        return overrideSize(super.getMinimumSize(), componentMinimumSize);
    }

    @Override
    public Dimension getMaximumSize() {
        return overrideSize(super.getMaximumSize(), componentMaximumSize);
    }

    @Override
    public Dimension getPreferredSize() {
        return overrideSize(super.getPreferredSize(), componentPreferredSize);
    }

    public Dimension getMaxPreferredSize() {
        return getPreferredSize();
    }
}
