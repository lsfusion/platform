package platform.client.form.queries;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

class QueryConditionComboBox extends JComboBox {

    public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        dim.height = QueryConditionView.PREFERRED_HEIGHT;
        return dim;
    }

    public QueryConditionComboBox(Vector<?> objects) {
        super(objects);
    }

    public QueryConditionComboBox(Object[] objects) {
        super(objects);
    }
}
