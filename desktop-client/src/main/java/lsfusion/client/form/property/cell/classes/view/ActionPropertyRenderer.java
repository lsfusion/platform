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
            button = property != null ? new JButton(ClientImages.getImage(property.design.getImageHolder())) : new JButton();
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
}
