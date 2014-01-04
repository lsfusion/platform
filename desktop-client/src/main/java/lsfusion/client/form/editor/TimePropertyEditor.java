package lsfusion.client.form.editor;

import com.toedter.calendar.JSpinnerDateEditor;
import lsfusion.client.form.ClientPropertyTableEditorComponent;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.cell.PropertyTableCellEditor;
import lsfusion.interop.ComponentDesign;
import lsfusion.interop.KeyStrokes;

import javax.swing.*;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.EventObject;

public class TimePropertyEditor extends JSpinnerDateEditor implements PropertyEditor, ClientPropertyTableEditorComponent {
    private PropertyTableCellEditor tableEditor;

    private final SimpleDateFormat format;

    private boolean turnToNull = false;

    public TimePropertyEditor(Object value, SimpleDateFormat format, ComponentDesign design) {
        this.format = format;
        if (design != null) {
            design.designCell(this);
        }

        setEditor(new TimePropertyEditorComponent(this, format.toPattern()));

        setBorder(null);

        setModel(new SpinnerDateModel());

        if (value != null)
            setValue(value);
    }

    @Override
    public boolean processKeyBinding(final KeyStroke ks, final KeyEvent ke, final int condition, final boolean pressed) {
        // передаем вниз нажатую клавишу, чтобы по нажатию кнопки она уже начинала вводить в объект
        if (condition == WHEN_FOCUSED) {
            if (ke.getKeyCode() == KeyEvent.VK_DELETE) {
                turnToNull = true;
                commitEditing();
                return true;
            }

            // передаём её прямо в JFormattedTextField
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ((TimePropertyEditorComponent.TimeTextField) getEditorComponent().getTextField()).processKeyBinding(ks, ke, condition, pressed);
                }
            });
            return KeyStrokes.isSuitableNumberEditEvent(ke);
        } else
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
        if (turnToNull) {
            turnToNull = false;
            return null;
        }

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

    @Override
    public void prepareTextEditor(boolean clearText) {
        if (clearText) {
            getEditorComponent().getTextField().setText("");
        } else {
            getEditorComponent().getTextField().selectAll();
        }
    }

    class TimePropertyEditorComponent extends JSpinner.DateEditor {
        public TimePropertyEditorComponent(JSpinner spinner, String pattern) {
            super(spinner, pattern);

            JFormattedTextField oldField = getTextField();
            remove(oldField);

            // для доступа к TextField'у подменяем своим добавленный в конструкторе, инициализируя аналогичным образом
            JFormattedTextField ftf = new TimeTextField() {
                @Override
                protected void processFocusEvent(FocusEvent e) {
                    // JFormattedTextField.processFocusEvent вызывает setValue(), который убивает эффект selectAll(),
                    // поэтому приходится ещё раз вызывать здесь
                    int selectionStart = getSelectionStart();
                    int selectionEnd = getSelectionEnd();
                    int docLength = getDocument().getLength();

                    super.processFocusEvent(e);

                    if (docLength == selectionEnd - selectionStart) {
                        selectAll();
                    }
                }
            };
            ftf.setName("Spinner.formattedTextField");
            ftf.setValue(spinner.getValue());
            ftf.addPropertyChangeListener(this);
            ftf.setEditable(true);
            ftf.setInheritsPopupMenu(true);

            String toolTipText = spinner.getToolTipText();
            if (toolTipText != null) {
                ftf.setToolTipText(toolTipText);
            }
            ftf.setFormatterFactory(new DefaultFormatterFactory(new DateEditorFormatter(format)));

            add(ftf);
            ActionMap ftfMap = ftf.getActionMap();

            if (ftfMap != null) {
                ftfMap.put("increment", oldField.getActionMap().get("increment"));
                ftfMap.put("decrement", oldField.getActionMap().get("decrement"));
            }
        }

        @Override
        public boolean requestFocusInWindow() {
            return getTextField().requestFocusInWindow();
        }

        class TimeTextField extends JFormattedTextField {
            @Override
            public boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE && getTextField().getText().equals(getTextField().getSelectedText())) {
                    turnToNull = true;
                    commitEditing();
                    return true;
                }

                // не ловим ввод, чтобы его словил сам JTable и обработал
                return e.getKeyCode() != KeyEvent.VK_ENTER && super.processKeyBinding(ks, e, condition, pressed);
            }
        }

        class DateEditorFormatter extends DateFormatter {

            DateEditorFormatter(DateFormat format) {
                super(format);
                setAllowsInvalid(false);
                setOverwriteMode(true);
                setCommitsOnValidEdit(true);
            }

            public void setMinimum(Comparable min) {
                getModel().setStart(min);
            }

            public Comparable getMinimum() {
                return getModel().getStart();
            }

            public void setMaximum(Comparable max) {
                getModel().setEnd(max);
            }

            public Comparable getMaximum() {
                return getModel().getEnd();
            }
        }
    }
}
