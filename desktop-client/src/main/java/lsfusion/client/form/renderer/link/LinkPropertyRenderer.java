package lsfusion.client.form.renderer.link;

import lsfusion.client.form.renderer.LabelPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;

public abstract class LinkPropertyRenderer extends LabelPropertyRenderer {
    public LinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);

        setHorizontalAlignment(JLabel.CENTER);
        setVerticalAlignment(JLabel.CENTER);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setText(null);
        }
        super.setValue(getImageIcon(), isSelected, hasFocus); // передаём суперу иконку, а не ссылку. из наличия ссылки не следует наличие иконки
    }
    
    protected ImageIcon getImageIcon() {
        return (ImageIcon) getIcon();
    }
}