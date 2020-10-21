package lsfusion.client.form.property.cell.view;

import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;

// renderer идет в виде label
public abstract class LabelPropertyRenderer extends PropertyRenderer {
    private JLabel label;

    protected LabelPropertyRenderer(ClientPropertyDraw property) {
        super(property);
        
        getComponent().setOpaque(true);
        
        if (property != null) {
            Integer valueAlignment = property.getSwingValueAlignment();
            if (valueAlignment != null) {
                getComponent().setHorizontalAlignment(valueAlignment);
            }
        }
    }

    @Override
    public JLabel getComponent() {
        if (label == null) {
            label = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    paintLabelComponent(g);
                }

                @Override
                public void setText(String text) {
                    super.setText("<html><body><nobr>" + text + "</nobr></body></html>");
                }
            };
        }
        return label;
    }
    
    public void paintLabelComponent(Graphics g) {}

    @Override
    protected boolean showRequiredString() {
        return true;
    }

    public void setValue(Object value) {
        super.setValue(value);
        if (value == null && property != null && property.isEditableNotNull()) {
            getComponent().setText(REQUIRED_STRING);
        }
    }
}
