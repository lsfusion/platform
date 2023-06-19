package lsfusion.client.form.property.cell.classes.view.link;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.LabelPropertyRenderer;

import javax.swing.*;

public class LinkPropertyRenderer extends LabelPropertyRenderer {
    private String extension;

    public LinkPropertyRenderer(ClientPropertyDraw property) {
        this(property, null);
    }

    public LinkPropertyRenderer(ClientPropertyDraw property, String extension) {
        super(property);
        this.extension = extension;

        getComponent().setVerticalAlignment(JLabel.CENTER);
    }

    public void setValue(Object value) {
        if (value != null) {
            getComponent().setText(null);
            getComponent().setIcon(extension != null ? SwingUtils.getSystemIcon(extension) : null);
        } else {
            getComponent().setIcon(null);
        }
        super.setValue(getImageIcon()); // передаём суперу иконку, а не ссылку. из наличия ссылки не следует наличие иконки
    }
    
    protected ImageIcon getImageIcon() {
        return (ImageIcon) getComponent().getIcon();
    }
}