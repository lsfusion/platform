package lsfusion.client.form.property.cell.classes.controller;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import lsfusion.client.classes.data.ClientIntervalClass;
import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.EventObject;

import static lsfusion.base.DateConverter.getIntervalPart;

public class IntervalPropertyEditor extends JDateChooser implements PropertyEditor {

    private static boolean left = false;
    private static boolean right = false;
    protected final Object defaultValue;
    private final ClientIntervalClass intervalClass;


    public IntervalPropertyEditor(Object value, boolean addButtons, ClientIntervalClass intervalClass) {
        super(null, null, new IntervalPropertyEditorComponent(value, intervalClass));
        this.defaultValue = value;
        this.intervalClass = intervalClass;

        configureButtons(addButtons);
    }

    private void configureButtons(boolean addButtons) {
        if (addButtons) {
            JButton leftButton = new JButton(new ImageIcon(this.getClass().getResource("/com/toedter/calendar/images/JDateChooserIcon.gif")));
            leftButton.setMargin(new Insets(0, 0, 0, 0));
            leftButton.addActionListener(this);
            this.add(leftButton, "West");
            leftButton.setMargin(new Insets(0, 0, 0, 0));

            leftButton.addActionListener(actionEvent -> left = true);
            calendarButton.addActionListener(actionEvent -> right = true);
        } else
            calendarButton.setVisible(false);
    }

    @Override
    public void setTableEditor(PropertyTableCellEditor tableEditor) {

    }

    @Override
    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return this;
    }

    @Override
    public Object getCellEditorValue() {
        try {
            IntervalPropertyEditorComponent dateEditor = (IntervalPropertyEditorComponent) getDateEditor();
            String text = dateEditor.getText();
            if (text.isEmpty())
                return null;
            BigDecimal parsedValue = (BigDecimal) intervalClass.parseString(text);
            return parsedValue != null ? parsedValue : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public boolean stopCellEditing() {
        cleanup();
        return true;
    }

    @Override
    public void cancelCellEditing() {
        cleanup();
    }

    protected static class IntervalPropertyEditorComponent extends JTextFieldDateEditor {

        private Object defaultValue;
        private final ClientIntervalClass intervalClass;

        public IntervalPropertyEditorComponent(Object value, ClientIntervalClass intervalClass) {
            this.defaultValue = value;
            this.intervalClass = intervalClass;
        }

        @Override
        public void setText(String t) {
            if (t.split(" - ").length > 1)
                super.setText(t);
        }

        @Override
        public Date getDate() {
            if (defaultValue != null) {
                if (left)
                    return new Date(getIntervalPart(defaultValue, true) * 1000);
                else if (right)
                    return new Date(getIntervalPart(defaultValue, false) * 1000);
            }
            return null;
        }

        @Override
        public void setDate(Date date) {
            if (date != null) {
                String s = String.valueOf(defaultValue);
                long l = date.getTime() / 1000;
                String[] split = s.split("\\.");

                if (split.length < 2) //there is no interval, only one date or there are some errors on input
                    s = String.valueOf(l);
                else if (left)
                    s = s.replaceFirst(split[0], String.valueOf(l));
                else if (right)
                    s = s.replaceAll("." + split[1], "." + l);

                defaultValue = s;
            }

            setValue(defaultValue != null ? intervalClass.formatString(defaultValue) : "");

            left = right = false;
        }
    }
}
