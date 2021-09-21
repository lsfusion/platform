package lsfusion.client.form.design.view.widget;

import lsfusion.client.form.design.view.FlexPanel;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.function.Supplier;

public class ScrollPaneWidget extends JScrollPane implements Widget {

    public ScrollPaneWidget(Component view) {
        super(view);

        Widget.addMouseListeners(this);
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    // for a scrollpane opposite fixed direction we want to have actual size instead of flex-basis size (web browser does that)
    public FlexPanel wrapFlexPanel;

    @Override
    public Dimension getPreferredSize() {
        if(wrapFlexPanel != null) {
//            assert oppositeStretchFixedFlexPanel == getViewport().getView();
            assert wrapFlexPanel.wrapScrollPane == this;
            return preferredLayoutSize(this, wrapFlexPanel::getDefaultPreferredSize); // we call getDefaultPreferredSize to use getPreferredSize instead of getFlexPreferredSize in FlexPanel
        }
        return super.getPreferredSize();
    }

    @Override
    public void checkMouseEvent(MouseEvent event) {
        int eventType = event.getID();
        if (eventType == MouseEvent.MOUSE_WHEEL && !event.isConsumed()) {
            Component c = event.getComponent();
            if (!c.equals(this)) {
                int dir = ((MouseWheelEvent)event).getWheelRotation();

                BoundedRangeModel m = getVerticalScrollBar().getModel();
                int extent = m.getExtent();
                int minimum = m.getMinimum();
                int maximum = m.getMaximum();
                int value = m.getValue();
                if (!(value + extent >= maximum && dir > 0 || value <= minimum && dir < 0)) {
                    dispatchEvent(SwingUtilities.convertMouseEvent(c, event, this));
                }
            }
        }
    }

    @Override
    public String toString() {
        return Widget.toString(this, super.toString());
    }

    // partial copy of ScrollPaneLayout
    private Dimension preferredLayoutSize(Container parent, Supplier<Dimension> preferredSize)
    {
        /* Sync the (now obsolete) policy fields with the
         * JScrollPane.
         */
        JScrollPane scrollPane = (JScrollPane)parent;

        Insets insets = parent.getInsets();
        int prefWidth = insets.left + insets.right;
        int prefHeight = insets.top + insets.bottom;

        /* Note that viewport.getViewSize() is equivalent to
         * viewport.getView().getPreferredSize() modulo a null
         * view or a view whose size was explicitly set.
         */

        Dimension extentSize = null;

        if (viewport != null)
            extentSize = preferredSize.get();

        /* If there's a viewport add its preferredSize.
         */

        if (extentSize != null) {
            prefWidth += extentSize.width;
            prefHeight += extentSize.height;
        }

        /* If there's a JScrollPane.viewportBorder, add its insets.
         */

        Border viewportBorder = scrollPane.getViewportBorder();
        if (viewportBorder != null) {
            Insets vpbInsets = viewportBorder.getBorderInsets(parent);
            prefWidth += vpbInsets.left + vpbInsets.right;
            prefHeight += vpbInsets.top + vpbInsets.bottom;
        }

        /* If a header exists and it's visible, factor its
         * preferred size in.
         */

        JViewport rowHead = getRowHeader();
        if ((rowHead != null) && rowHead.isVisible()) {
            prefWidth += rowHead.getPreferredSize().width;
        }

        JViewport colHead = getRowHeader();
        if ((colHead != null) && colHead.isVisible()) {
            prefHeight += colHead.getPreferredSize().height;
        }

        return new Dimension(prefWidth, prefHeight);
    }
}
