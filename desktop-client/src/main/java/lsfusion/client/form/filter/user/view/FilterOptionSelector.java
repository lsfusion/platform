package lsfusion.client.form.filter.user.view;

import lsfusion.base.BaseUtils;
import lsfusion.client.base.view.SwingDefaults;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import static javax.swing.BorderFactory.*;

public abstract class FilterOptionSelector<T> extends JTextField {
    JPopupMenu menu = new JPopupMenu();
    protected T currentValue;

    public FilterOptionSelector() {
        this(Collections.emptyList());
    }
    
    public FilterOptionSelector(List<T> items) {
        setEditable(false);
        setFocusable(false);

        setBorder(createCompoundBorder(
                createCompoundBorder(
                        createEmptyBorder(0, 0, 0, 2),
                        createLineBorder(SwingDefaults.getComponentBorderColor())),
                createEmptyBorder(0, SwingDefaults.getTableCellMargins().left, 0, 0)));

        for (T item : items) {
            addMenuItem(item, item.toString());
        }
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showMenu();
            }
        });
    }

    public void add(T value, String caption) {
        addMenuItem(value, caption);
    }

    protected JMenuItem addMenuItem(T item, String caption) {
        JMenuItem menuItem = new JMenuItem(caption);
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

    public void showMenu() {
        menu.show(this, 0, getHeight());
    }

    public abstract void valueChanged(T value);
}
