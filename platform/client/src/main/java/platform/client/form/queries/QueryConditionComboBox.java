package platform.client.form.queries;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

class QueryConditionComboBox extends JComboBox {

    public Dimension getPreferredSize() {
//        Dimension dim = super.getPreferredSize();
//        dim.height = QueryConditionView.PREFERRED_HEIGHT;
//        return dim;
        return new Dimension(super.getPreferredSize().width, QueryConditionView.PREFERRED_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
//        return super.getMinimumSize();
        return new Dimension(30, super.getMinimumSize().height);
    }

    public QueryConditionComboBox(Vector<?> objects) {
        super(objects);
    }

    public QueryConditionComboBox(Object[] objects) {
        super(objects);
    }
}
