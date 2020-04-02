package lsfusion.client.form.filter.user.view;

import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.view.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

class QueryConditionComboBox extends JComboBox {

    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, MainFrame.getIntUIFontSize(SwingDefaults.getComponentHeight()));
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(30, super.getMinimumSize().height);
    }

    public QueryConditionComboBox(Vector<?> objects) {
        super(objects);
    }

    public QueryConditionComboBox(Object[] objects) {
        super(objects);
    }
}
