package lsfusion.client.form.filter.user.view;

import lsfusion.base.BaseUtils;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.design.view.widget.TextFieldWidget;
import lsfusion.client.form.object.table.controller.TableController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import static javax.swing.BorderFactory.*;

public abstract class FilterOptionSelector<T> extends TextFieldWidget {
    JScrollPopupMenu menu = new JScrollPopupMenu();
    protected T currentValue;
    private TableController logicsSupplier;

    public FilterOptionSelector(TableController logicsSupplier) {
        this(logicsSupplier, Collections.emptyList(), Collections.emptyList());
    }
    
    public FilterOptionSelector(TableController logicsSupplier, List<T> items, List<String> popupCaptions) {
        this.logicsSupplier = logicsSupplier;
        
        setEditable(false);
        setFocusable(false);
        
        menu.setMaximumVisibleRows(12);

        for (T item : items) {
            addMenuItem(item, item.toString(), popupCaptions.get(items.indexOf(item)));
        }
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                onMousePressed();
            }
        });
    }

    public void add(T value, String caption) {
        add(value, caption, caption);
    }

    public void add(T value, String caption, String popupCaption) {
        addMenuItem(value, caption, popupCaption);
    }

    protected JMenuItem addMenuItem(T item, String caption, String popupCaption) {
        JMenuItem menuItem = new JMenuItem(popupCaption);
        menuItem.addActionListener(e -> {
            setSelectedValue(item, caption);
            valueChanged(item);
        });
        menu.add(menuItem);
        
        return menuItem;
    }

    public void setSelectedValue(T value) {
        setSelectedValue(value, value != null ? value.toString() : null);
    }

    public void setSelectedValue(T value, String caption) {
        currentValue = value;
        setText(caption);
    }

    @Override
    public void setText(String text) {
        String notNullText = BaseUtils.nullTrim(text);
        super.setText(notNullText);
        setToolTipText(notNullText);
    }

    @Override
    public Dimension getPreferredSize() {
        Insets insets = SwingDefaults.getTableCellMargins();
        int width = getFontMetrics(getFont()).stringWidth(getText()) + insets.left + insets.right + SwingDefaults.getComponentBorderWidth() * 2 + 2; // 2 for outer empty border (margin)
        return new Dimension(width, SwingDefaults.getComponentHeight());
    }

    public void onMousePressed() {
        logicsSupplier.getFormController().commitCurrentEditing();
        
        menu.show(this, 0, getHeight());
    }

    @Override
    public void updateUI() {
        super.updateUI();
        
        setBorder(createCompoundBorder(
                createCompoundBorder(
                        createEmptyBorder(0, 0, 0, 2),
                        createLineBorder(SwingDefaults.getComponentBorderColor())),
                createEmptyBorder(0, SwingDefaults.getTableCellMargins().left, 0, 0)));
    }

    public void hidePopup() {
        menu.setVisible(false);
    }

    public abstract void valueChanged(T value);
}
