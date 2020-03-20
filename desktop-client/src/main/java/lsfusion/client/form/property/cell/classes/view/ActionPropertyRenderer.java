package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.PropertyRenderer;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ActionPropertyRenderer extends PropertyRenderer {
    private static final String defaultCaption = "...";
    private Icon defaultIcon;

    private JButton button;

    public ActionPropertyRenderer(ClientPropertyDraw property) {
        super(property);

        getComponent().setFocusPainted(false);
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
            property.design.designButton(getComponent(), MainController.colorTheme);
        }
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
        if (defaultIcon == null && getComponent().getIcon() != null) defaultIcon = getComponent().getIcon(); // временно так

        getComponent().setText(defaultIcon != null || value == null ? "" : defaultCaption);
        getComponent().setIcon(value == null ? null : defaultIcon);
    }
}
