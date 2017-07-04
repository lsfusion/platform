package lsfusion.client.form.layout;

import lsfusion.client.logics.ClientComponent;

import javax.swing.*;
import java.awt.*;

import static lsfusion.client.SwingUtils.overrideSize;

/**
 * Created by User on 04.07.2017.
 */
public class JComponentPanel extends JPanel {
    
    public JComponentPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    public JComponentPanel(LayoutManager layout) {
        super(layout);
    }
    
    public JComponentPanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }
    
    public JComponentPanel() {
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

}
