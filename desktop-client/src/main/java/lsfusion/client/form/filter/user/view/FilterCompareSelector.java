package lsfusion.client.form.filter.user.view;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.interop.form.property.Compare;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public abstract class FilterCompareSelector extends FilterOptionSelector<Compare> {
    private boolean negation;
    private JCheckBox negationCB;

    private boolean allowNull;
    private JCheckBox allowNullCB;
    
    public FilterCompareSelector(ClientPropertyFilter condition) {
        super(Arrays.asList(condition.property.getFilterCompares()));
        negation = condition.negation;

        negationCB = new JCheckBox("!");
        negationCB.setSelected(negation);
        negationCB.addActionListener(event -> {
            negation = negationCB.isSelected();
            negationChanged(negation);
            updateText();
        });
        
        allowNullCB = new JCheckBox(ClientResourceBundle.getString("form.queries.filter.condition.allow.null"));
        allowNullCB.setSelected(allowNull);
        allowNullCB.addActionListener(event -> {
            allowNull = allowNullCB.isSelected();
            allowNullChanged(allowNull);
        });

        addOptions();
    }

    public void set(List<Compare> values) {
        menu.removeAll();
        for (Compare value : values) {
            addMenuItem(value, value.toString());
        }
        addOptions();
    }
    
    public void addOptions() {
        int newItemIndex = menu.getComponentCount();
        menu.insert(new JPopupMenu.Separator(), newItemIndex++);
        menu.insert(negationCB, newItemIndex++);
        menu.insert(allowNullCB, newItemIndex++);
    }

    @Override
    public void valueChanged(Compare value) {
        updateText();
    }

    @Override
    public void setText(String text) {
        super.setText((negation ? "!" : "") + text);
    }

    private void updateText() {
        setText(currentValue.toString());
    }

    public abstract void negationChanged(boolean value);
    public abstract void allowNullChanged(boolean value);
}
