package lsfusion.client.form.property.cell.classes.controller;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import lsfusion.client.classes.data.ClientDateIntervalClass;
import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventObject;

public class IntervalPropertyEditor extends JDateChooser implements PropertyEditor {

    private static boolean left = false;
    private static boolean right = false;
    protected final Object defaultValue;
    private final SimpleDateFormat format;

    public IntervalPropertyEditor(Object value, SimpleDateFormat format, boolean addButtons) {
        super(null, null, format.toPattern(), new IntervalPropertyEditorComponent(value, format));
        this.defaultValue = value;
        this.format = format;

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
            String[] dates = dateEditor.getText().split(" - ");
            long timeFrom = format.parse(dates[0]).getTime();
            long timeTo = format.parse(dates[1]).getTime();
            return timeTo >= timeFrom ? new BigDecimal(timeFrom / 1000 + "." + timeTo / 1000) : defaultValue;
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

        private final SimpleDateFormat editingFormat;
        private Object defaultValue;

        public IntervalPropertyEditorComponent(Object value, SimpleDateFormat editingFormat) {
            this.defaultValue = value;
            this.editingFormat = editingFormat;
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
                    return ClientDateIntervalClass.getDateFromInterval(defaultValue, true);
                else if (right)
                    return ClientDateIntervalClass.getDateFromInterval(defaultValue, false);
            }
            return null;
        }

        @Override
        public void setDate(Date date) {
            if (date != null) {
                String s = String.valueOf(defaultValue);
                long l = date.getTime() / 1000;
                String[] split = s.split("\\.");

                if (left)
                    s = s.replaceFirst(split[0], String.valueOf(l));
                else if (right)
                    s = s.replaceAll("." + split[1], "." + l);

                defaultValue = s;
            }

            setValue(defaultValue != null ? editingFormat.format(ClientDateIntervalClass.getDateFromInterval(defaultValue, true))
                    + " - " + editingFormat.format(ClientDateIntervalClass.getDateFromInterval(defaultValue, false)) : "");

            left = right = false;
        }
    }
}
