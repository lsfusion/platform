package lsfusion.client.form.design.view;

import lsfusion.client.form.design.view.widget.ComponentWidget;
import lsfusion.client.form.design.view.widget.Widget;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import java.awt.*;
import java.beans.ConstructorProperties;

public class Filler extends ComponentWidget implements Accessible {

    public static Widget createHorizontalStrut(int width) {
        return new Filler(new Dimension(width,0), new Dimension(width,0),
                new Dimension(width, Short.MAX_VALUE));
    }

    /**
     * Constructor to create shape with the given size ranges.
     *
     * @param min   Minimum size
     * @param pref  Preferred size
     * @param max   Maximum size
     */
    @ConstructorProperties({"minimumSize", "preferredSize", "maximumSize"})
    public Filler(Dimension min, Dimension pref, Dimension max) {
        setMinimumSize(min);
        setPreferredSize(pref);
        setMaximumSize(max);
    }

    /**
     * Change the size requests for this shape.  An invalidate() is
     * propagated upward as a result so that layout will eventually
     * happen with using the new sizes.
     *
     * @param min   Value to return for getMinimumSize
     * @param pref  Value to return for getPreferredSize
     * @param max   Value to return for getMaximumSize
     */
    public void changeShape(Dimension min, Dimension pref, Dimension max) {
        setMinimumSize(min);
        setPreferredSize(pref);
        setMaximumSize(max);
        revalidate();
    }

    // ---- Component methods ------------------------------------------

    /**
     * Paints this <code>Filler</code>.  If this
     * <code>Filler</code> has a UI this method invokes super's
     * implementation, otherwise if this <code>Filler</code> is
     * opaque the <code>Graphics</code> is filled using the
     * background.
     *
     * @param g the <code>Graphics</code> to paint to
     * @throws NullPointerException if <code>g</code> is null
     * @since 1.6
     */
    protected void paintComponent(Graphics g) {
        if (ui != null) {
            // On the off chance some one created a UI, honor it
            super.paintComponent(g);
        } else if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

/////////////////
// Accessibility support for Box$Filler
////////////////

    /**
     * Gets the AccessibleContext associated with this Box.Filler.
     * For box fillers, the AccessibleContext takes the form of an
     * AccessibleBoxFiller.
     * A new AccessibleAWTBoxFiller instance is created if necessary.
     *
     * @return an AccessibleBoxFiller that serves as the
     *         AccessibleContext of this Box.Filler.
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new Filler.AccessibleBoxFiller();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>Box.Filler</code> class.
     */
    @SuppressWarnings("serial")
    protected class AccessibleBoxFiller extends AccessibleAWTComponent {
        // AccessibleContext methods
        //
        /**
         * Gets the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of
         *   the object (AccessibleRole.FILLER)
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.FILLER;
        }
    }
}