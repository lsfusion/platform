package lsfusion.client.form.property.cell.classes.view.link;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;

public class TableLinkPropertyRenderer extends LinkPropertyRenderer {

    public TableLinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        if (value != null) {
            getComponent().setIcon(SwingUtils.getSystemIcon("table"));
        }
        super.setValue(value);
    }
}