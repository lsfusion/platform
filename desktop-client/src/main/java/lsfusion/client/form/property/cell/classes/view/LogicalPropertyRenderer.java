package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.PropertyRenderer;

import javax.swing.*;
import java.awt.*;

public class LogicalPropertyRenderer extends PropertyRenderer {
    private JCheckBox checkBox;
    private boolean threeState;
    
    public LogicalPropertyRenderer(ClientPropertyDraw property, boolean threeState) {
        super(property);
        this.threeState = threeState;
        
        if (property != null) {
            Integer valueAlignment = property.getSwingValueAlignment();
            if (valueAlignment != null) {
                getComponent().setHorizontalAlignment(valueAlignment);
            }
        }
        
        getComponent().setBorderPainted(true);
        getComponent().setOpaque(true);
    }

    public JCheckBox getComponent() {
        if (checkBox == null) {
            checkBox = new JCheckBox() {
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
        return checkBox;
    }

    public void setValue(Object value) {
        super.setValue(value);
        getComponent().setSelected(value != null && (Boolean) value);
        getComponent().setEnabled(!threeState || value != null);
    }

    @Override
    public boolean isAutoDynamicHeight() {
        return false;
    }
}
