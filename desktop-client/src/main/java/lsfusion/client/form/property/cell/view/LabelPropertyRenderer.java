package lsfusion.client.form.property.cell.view;

import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;
import static lsfusion.client.form.controller.ClientFormController.colorPreferences;

// renderer идет в виде label
public abstract class LabelPropertyRenderer extends PropertyRenderer {
    protected Color defaultForeground = NORMAL_FOREGROUND;
    
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
        
            
        defaultForeground = getComponent().getForeground();
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
            };
        }
        return label;
    }
    
    public void paintLabelComponent(Graphics g) {}

    @Override
    protected void drawForeground(Color conditionalForeground) {
        if (value == null) {
            if (property != null && property.isEditableNotNull()) {
                getComponent().setForeground(REQUIRED_FOREGROUND);
            }
        } else {
            getComponent().setForeground(conditionalForeground != null ? conditionalForeground : defaultForeground);
        }
    }

    @Override
    protected Border getDefaultBorder() {
        return createEmptyBorder(1, 2, 1, 2);
    }

    @Override
    protected void drawBorder(boolean isInFocusedRow, boolean hasFocus) {
        if (hasFocus) {
            getComponent().setBorder(createCompoundBorder(colorPreferences.getFocusedCellBorder(), createEmptyBorder(0, 1, 0, 1)));
        } else if (isInFocusedRow) {
            getComponent().setBorder(createCompoundBorder(colorPreferences.getSelectedRowBorder(), getDefaultBorder()));
        } else {
            getComponent().setBorder(getDefaultBorder());
        }
    }

    public void setValue(Object value) {
        super.setValue(value);
        if (value == null && property != null && property.isEditableNotNull()) {
            getComponent().setText(REQUIRED_STRING);
        }
    }
}
