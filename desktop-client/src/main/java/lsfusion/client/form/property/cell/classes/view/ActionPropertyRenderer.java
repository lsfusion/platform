package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.base.view.ClientImages;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.PropertyRenderer;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ActionPropertyRenderer extends PropertyRenderer {
    private static final String ICON_EXECUTE = "action.png";
    private Icon defaultIcon;

    private JButton button;

    public ActionPropertyRenderer(ClientPropertyDraw property) {
        super(property);

        getComponent().setFocusPainted(false);
    }

    public JButton getComponent() {
        if (button == null) {
            button = new JButton(property != null ? ClientImages.getImage(property.design.getImage()) : null) {
                /**
                 * Overridden for performance reasons. Copied from DefaultTableCellRenderer
                 */
                public void invalidate() {}
                public void validate() {}
                public void revalidate() {}

                public void repaint(long tm, int x, int y, int width, int height) {}
                public void repaint(Rectangle r) { }
                public void repaint() { }
            };
        }
        return button;
    }

    @Override
    public Color getDefaultBackground() {
        return SwingDefaults.getButtonBackground();
    }

    @Override
    protected Border getDefaultBorder() {
        return SwingDefaults.getButtonBorder();
    }

    public void setValue(Object value) {
        super.setValue(value);
        if (defaultIcon == null) {
            defaultIcon = getComponent().getIcon();
            if(defaultIcon == null) {
                defaultIcon = ClientImages.get(ICON_EXECUTE);
            }
        }
        getComponent().setIcon(value == null ? null : defaultIcon);
    }

    @Override
    public boolean isAutoDynamicHeight() {
        return false;
    }
}
