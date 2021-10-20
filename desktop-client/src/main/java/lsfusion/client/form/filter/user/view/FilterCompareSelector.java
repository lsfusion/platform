package lsfusion.client.form.filter.user.view;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.interop.form.property.Compare;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

public abstract class FilterCompareSelector extends FilterOptionSelector<Compare> {
    private boolean negation;
    private JCheckBox negationCB;

    private boolean allowNull;
    private JCheckBox allowNullCB;
    
    public FilterCompareSelector(ClientPropertyFilter condition, boolean allowNull) {
        super(Arrays.asList(condition.property.getFilterCompares()));
        negation = condition.negation;
        this.allowNull = allowNull;

        negationCB = new JCheckBox("!");
        negationCB.setSelected(negation);
        negationCB.addActionListener(event -> {
            negation = negationCB.isSelected();
            negationChanged(negation);
            updateText();
        });
        
        allowNullCB = new JCheckBox(ClientResourceBundle.getString("form.queries.filter.condition.allow.null")) {
            @Override
            public Dimension getPreferredSize() {
                Dimension preferredSize = super.getPreferredSize();
                // doesn't have enough space together with scrollable menu   
                return new Dimension(preferredSize.width + 2, preferredSize.height);
            }
        };
        allowNullCB.setSelected(this.allowNull);
        allowNullCB.addActionListener(event -> {
            this.allowNull = allowNullCB.isSelected();
            allowNullChanged(this.allowNull);
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
    protected JMenuItem addMenuItem(Compare item, String caption) {
        JMenuItem menuItem = super.addMenuItem(item, caption);
        menuItem.setToolTipText(item.getTooltipText());
        return menuItem;
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

    @Override
    public String getToolTipText(MouseEvent event) {
        return (negation ? "!" : "") + currentValue.getTooltipText();
    }

    public abstract void negationChanged(boolean value);
    public abstract void allowNullChanged(boolean value);
}
