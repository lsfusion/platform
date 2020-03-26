package lsfusion.client.form.property.cell.classes.view.link;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.LabelPropertyRenderer;

import javax.swing.*;

public abstract class LinkPropertyRenderer extends LabelPropertyRenderer {
    public LinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);

        getComponent().setVerticalAlignment(JLabel.CENTER);
    }

    public void setValue(Object value) {
        if (value != null) {
            getComponent().setText(null);
        } else {
            getComponent().setIcon(null);
        }
        super.setValue(getImageIcon()); // передаём суперу иконку, а не ссылку. из наличия ссылки не следует наличие иконки
    }
    
    protected ImageIcon getImageIcon() {
        return (ImageIcon) getComponent().getIcon();
    }
}