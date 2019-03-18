package lsfusion.client.form.property.classes.renderer;

import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ActionPropertyRenderer extends PropertyRenderer {
    private static final String defaultCaption = "...";
    private Icon defaultIcon;
    private Border defaultBorder;

    private JButton button;

    public ActionPropertyRenderer(ClientPropertyDraw property) {
        super(property);

        getComponent().setFocusPainted(false);
        
        defaultBorder = getComponent().getBorder();
    }

    public JButton getComponent() {
        if (button == null) {
            button = new JButton();
        }
        return button;
    }
    
    @Override
    protected void initDesign() {
        if (property != null) {
            property.design.designComponent(getComponent());
        }
        defaultBackground = getComponent().getBackground();
    }

    @Override
    protected Color getDefaultBackground() {
        return value == null ? Color.WHITE : super.getDefaultBackground();
    }

    @Override
    protected Border getDefaultBorder() {
        return value == null ? super.getDefaultBorder() : defaultBorder;
    }

    public void setValue(Object value) {
        super.setValue(value);
        if (defaultIcon == null && getComponent().getIcon() != null) defaultIcon = getComponent().getIcon(); // временно так

        getComponent().setText(defaultIcon != null || value == null ? "" : defaultCaption);
        getComponent().setIcon(value == null ? null : defaultIcon);
    }
}
