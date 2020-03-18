package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.view.MainFrame;

import javax.swing.*;
import javax.swing.border.Border;

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
            property.design.designButton(getComponent(), MainFrame.colorTheme);
        }
//        defaultBackground = getComponent().getBackground();
    }

//    @Override
//    protected Color getDefaultBackground() {
//        return value == null ? Color.WHITE : super.getDefaultBackground();
//    }

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
