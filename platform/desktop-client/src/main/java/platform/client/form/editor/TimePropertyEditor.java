package platform.client.form.editor;

import com.toedter.calendar.JSpinnerDateEditor;
import platform.client.form.PropertyEditor;
import platform.client.form.cell.PropertyTableCellEditor;
import platform.interop.ComponentDesign;

import javax.swing.*;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.EventObject;

public class TimePropertyEditor extends JSpinnerDateEditor implements PropertyEditor {
    private PropertyTableCellEditor tableEditor;

    private final SimpleDateFormat format;

    public TimePropertyEditor(Object value, SimpleDateFormat format, ComponentDesign design) {
        this.format = format;
        if (design != null) {
            design.designCell(this);
        }

        setEditor(new TimePropertyEditorComponent(this, format.toPattern()));

        setBorder(null);

        if (value != null)
            setValue(value);
    }

    @Override
    public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
        // передаем вниз нажатую клавишу, чтобы по нажатию кнопки она уже начинала вводить в объект
        if (condition == WHEN_FOCUSED) {
            if (ke.getKeyCode() == KeyEvent.VK_DELETE) {
                getEditorComponent().getTextField().setText("");
                commitEditing();
                return true;
            }
            return getEditorComponent().processKeyBinding(ks, ke, condition, pressed);
        }
        else
            return super.processKeyBinding(ks, ke, condition, pressed);
    }

    @Override
    public boolean requestFocusInWindow() {
        // пересылаем фокус в нужный объект
        return getEditor().requestFocusInWindow();
    }

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        this.tableEditor = tableEditor;
    }

    @Override
    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return this;
    }

    @Override
    public Object getCellEditorValue() {
        String text = getEditorComponent().getTextField().getText();
        if (text.isEmpty()) {
            return null;
        }
        return new Time(format.parse(text, new ParsePosition(0)).getTime());
    }

    private TimePropertyEditorComponent getEditorComponent() {
        return (TimePropertyEditorComponent) getEditor();
    }

    @Override
    public boolean stopCellEditing() {
        return true;
    }

    public void commitEditing() {
        if (!tableEditor.stopCellEditing()) {
            tableEditor.cancelCellEditing();
        }
    }

    class TimePropertyEditorComponent extends JSpinner.DateEditor {
        public TimePropertyEditorComponent(JSpinner spinner, String pattern) {
            super(spinner, pattern);

            getTextField().setFormatterFactory(new DefaultFormatterFactory(new DateEditorFormatter((SpinnerDateModel) spinner.getModel(), format)));
        }

        class DateEditorFormatter extends DateFormatter {
            private final SpinnerDateModel model;

            DateEditorFormatter(SpinnerDateModel model, DateFormat format) {
                super(format);
                this.model = model;
                setAllowsInvalid(false);
                setOverwriteMode(true);
                setCommitsOnValidEdit(true);
            }

            public void setMinimum(Comparable min) {
                model.setStart(min);
            }

            public Comparable getMinimum() {
                return model.getStart();
            }

            public void setMaximum(Comparable max) {
                model.setEnd(max);
            }

            public Comparable getMaximum() {
                return model.getEnd();
            }

            @Override
            public Object stringToValue(String text) throws ParseException {
                if (text.endsWith("  :  :  ")) {
                    return null;
                }
                return super.stringToValue(text);
            }
        }

        @Override
        protected boolean processKeyBinding(final KeyStroke ks, final KeyEvent e, final int condition, final boolean pressed) {
            if (e.getKeyCode() == KeyEvent.VK_DELETE && getTextField().getText().isEmpty()) {
                commitEditing();
                return true;
            }

            // не ловим ввод, чтобы его словил сам JTable и обработал
            return e.getKeyCode() != KeyEvent.VK_ENTER && super.processKeyBinding(ks, e, condition, pressed);
        }

        @Override
        public boolean requestFocusInWindow() {
            return getTextField().requestFocusInWindow();
        }
    }
}
