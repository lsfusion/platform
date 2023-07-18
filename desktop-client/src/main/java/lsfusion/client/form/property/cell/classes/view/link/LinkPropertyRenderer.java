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
        ImageIcon imageIcon = getImageIcon();
        if(imageIcon != null) {
            super.setValue(imageIcon);
            getComponent().setIcon(extension != null ? SwingUtils.getSystemIcon(extension) : null);
        } else {
            getComponent().setText(value != null ? "<html><a href=" + value + ">" + value + "</a>" : null);
        }
    }

    protected ImageIcon getImageIcon() {
        return null;
    }
}