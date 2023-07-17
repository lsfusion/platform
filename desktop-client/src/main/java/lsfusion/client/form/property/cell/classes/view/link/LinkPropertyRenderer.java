package lsfusion.client.form.property.cell.classes.view.link;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.LabelPropertyRenderer;

import javax.swing.*;

public class LinkPropertyRenderer extends LabelPropertyRenderer {
    public LinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);

        getComponent().setVerticalAlignment(JLabel.CENTER);
    }

    public void setValue(Object value) {
        ImageIcon imageIcon = getImageIcon();
        if(imageIcon != null) {
            super.setValue(imageIcon);
        } else {
            getComponent().setText(value != null ? "<html><a href=" + value + ">" + value + "</a>" : null);
        }
    }

    protected ImageIcon getImageIcon() {
        return null;
    }
}