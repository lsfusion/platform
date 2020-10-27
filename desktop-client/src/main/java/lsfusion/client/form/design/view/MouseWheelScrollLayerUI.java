package lsfusion.client.form.design.view;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.event.MouseWheelEvent;

public class MouseWheelScrollLayerUI extends LayerUI<JScrollPane> {
    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        if (c instanceof JLayer) {
            ((JLayer) c).setLayerEventMask(AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        }
    }

    @Override
    public void uninstallUI(JComponent c) {
        if (c instanceof JLayer) {
            ((JLayer) c).setLayerEventMask(0);
        }
        super.uninstallUI(c);
    }

    @Override
    protected void processMouseWheelEvent(MouseWheelEvent e, JLayer<? extends JScrollPane> l) {
        Component c = e.getComponent();
        int dir = e.getWheelRotation();
        JScrollPane main = l.getView();
        if (c instanceof JScrollPane && !c.equals(main)) {
            JScrollPane child = (JScrollPane) c;
            BoundedRangeModel m = child.getVerticalScrollBar().getModel();
            int extent = m.getExtent();
            int minimum = m.getMinimum();
            int maximum = m.getMaximum();
            int value = m.getValue();
            if (value + extent >= maximum && dir > 0 || value <= minimum && dir < 0) {
                main.dispatchEvent(SwingUtilities.convertMouseEvent(c, e, main));
            }
        }
    }
}
