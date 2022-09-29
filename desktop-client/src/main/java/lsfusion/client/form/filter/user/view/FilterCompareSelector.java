package lsfusion.client.form.filter.user.view;

import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.interop.form.property.Compare;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

import static lsfusion.client.ClientResourceBundle.getString;

public abstract class FilterCompareSelector extends FilterOptionSelector<Compare> {
    public final String NOT_STRING = getString("form.queries.filter.condition.not");
    
    private boolean negation;
    private JCheckBox negationCB;

    private boolean allowNull;
    private JCheckBox allowNullCB;
    
    public FilterCompareSelector(TableController logicsSupplier, ClientPropertyFilter condition, List<Compare> items, List<String> popupCaptions, boolean allowNull) {
        super(logicsSupplier, items, popupCaptions);
        negation = condition.negation;
        this.allowNull = allowNull;

        negationCB = new JCheckBox("! (" + NOT_STRING + ")");
        negationCB.setToolTipText(NOT_STRING);
        negationCB.setSelected(negation);
        negationCB.addActionListener(event -> {
            negation = negationCB.isSelected();
            negationChanged(negation);
            updateText();
        });
        
        allowNullCB = new JCheckBox(getString("form.queries.filter.condition.allow.null")) {
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
        
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                SwingUtilities.invokeLater(() -> menuCanceled());
            }
        });
    }

    public void set(List<Compare> values) {
        menu.removeAll();
        for (Compare value : values) {
            addMenuItem(value, value.toString(), value.getFullString());
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
    protected JMenuItem addMenuItem(Compare item, String caption, String popupCaption) {
        JMenuItem menuItem = super.addMenuItem(item, caption, popupCaption);
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
        return (negation ? NOT_STRING + " " : "") + currentValue.getTooltipText();
    }

    public abstract void negationChanged(boolean value);
    public abstract void allowNullChanged(boolean value);
    public abstract void menuCanceled();
}
