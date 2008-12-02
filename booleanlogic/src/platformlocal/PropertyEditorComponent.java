package platformlocal;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;

import javax.swing.*;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.text.NumberFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.DecimalFormat;

public interface PropertyEditorComponent {

    Component getComponent();

    Object getCellEditorValue();
    boolean valueChanged();

}


class TextFieldPropertyEditor extends JFormattedTextField {

    TextFieldPropertyEditor() {
        super();
        setBorder(new EmptyBorder(0, 3, 0, 0));
        setOpaque(true);
//        setBackground(new Color(128,128,255));
    }

    @Override
    public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {

        // не ловим ввод, чтобы его словил сам JTable и обработал
        return ke.getKeyCode() != KeyEvent.VK_ENTER && super.processKeyBinding(ks, ke, condition, pressed);
    }

}

class IntegerPropertyEditor extends TextFieldPropertyEditor
                            implements PropertyEditorComponent {

    public IntegerPropertyEditor(Object value, NumberFormat iformat, java.lang.Class<?> valueClass) {

        NumberFormat format = iformat;
        if (format == null)
            format = NumberFormat.getInstance();

        if (Double.class.equals(valueClass) && format instanceof DecimalFormat) {
            ((DecimalFormat) format).setDecimalSeparatorAlwaysShown(true);
        }

/*        if (format instanceof DecimalFormat) {
            ((DecimalFormat) format).setDecimalSeparatorAlwaysShown(true);
        }*/

        NumberFormatter formatter = new NumberFormatter(format) {

            public Object stringToValue(String text) throws ParseException {
                if (text.isEmpty() || text.equals("-") || text.equals(",") || text.equals(".") || text.equals("-,") || text.equals("-.")) return null;
                return super.stringToValue(text);
            }
        };
        
        formatter.setValueClass(valueClass);
        formatter.setAllowsInvalid(false);

        this.setHorizontalAlignment(JTextField.RIGHT);

        setFormatterFactory(new DefaultFormatterFactory(formatter));

        if (value != null)
            setValue(value);
        selectAll();

    }

    public Component getComponent() {
        return this;
    }

    public Object getCellEditorValue() {

        try {
            commitEdit();
        } catch (ParseException e) {
            return null;
        }

        Object value = this.getValue();

        return value;
/*        if (value instanceof Integer)
            return value;

        if (value instanceof Long)
            return ((Long) value).intValue();

        return null; */
    }

    public boolean valueChanged() {
        return true;
    }

}

class StringPropertyEditor extends TextFieldPropertyEditor
                           implements PropertyEditorComponent {

    public StringPropertyEditor(Object value) {
        super();

        if (value != null)
            setText(value.toString());
        selectAll();
    }

    public Component getComponent() {
        return this;
    }

    public Object getCellEditorValue() {
        if (getText().isEmpty()) return null;
        return getText();
    }

    public boolean valueChanged() {
        return true;
    }

}

class DatePropertyEditor extends JDateChooser
                           implements PropertyEditorComponent {

    public DatePropertyEditor(Object value) {
        super(null, null, "dd.MM.yy", new DatePropertyEditorComponent("dd.MM.yy","##.##.##",' '));

        if (value != null)
            setDate(DateConverter.intToDate((Integer)value));
        ((JFormattedTextField)dateEditor).selectAll();
    }

    @Override
    public void requestFocus() {
        // пересылаем фокус в нужный объект
        ((JFormattedTextField)dateEditor).requestFocusInWindow();
    }

    @Override
    public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {

        // передаем вниз нажатую клавишу, чтобы по нажатию кнопки она уже начинала вводить в объект
        if (condition == WHEN_FOCUSED)
            return ((DatePropertyEditorComponent)dateEditor).publicProcessKeyBinding(ks, ke, condition, pressed);
        else
            return super.processKeyBinding(ks, ke, condition, pressed);
    }

    @Override
    public void setNextFocusableComponent(Component comp) {
        super.setNextFocusableComponent(comp);
        ((JComponent)dateEditor).setNextFocusableComponent(comp);
//        jcalendar.setNextFocusableComponent(dateEditor.getUiComponent());


        // вот эту хрень приходится добавлять по той причине, что иначе так как popup вообще говоря не child таблицы,
        // то при нажатии на что угодно - она тут же делает stopEditing...
        if (comp instanceof JTable) {

            final JTable table = (JTable) comp;

            popup.addPopupMenuListener(new PopupMenuListener() {

                Boolean oldValue;

                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    oldValue = (Boolean)table.getClientProperty("terminateEditOnFocusLost");
                    table.putClientProperty("terminateEditOnFocusLost", Boolean.FALSE);
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    table.putClientProperty("terminateEditOnFocusLost", oldValue);
                }

                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });

        }

        // а вот эту хрень приходится добавлять потому что popupMenuWillBecomeInvisible срабатывает раньше чем
        // проверяется на изменение фокуса
        SwingUtils.removeFocusable(jcalendar);

        // к слову все равно это все дело очень хриво работает и все из-за долбанных popup'ов
    }

    public Component getComponent() {
        return this;
    }

    public Object getCellEditorValue() {

        return DateConverter.dateToInt(getDate());
    }

    public boolean valueChanged() {
        return true;
    }

}

class DatePropertyEditorComponent extends JTextFieldDateEditor {

    public DatePropertyEditorComponent(String datePattern, String maskPattern, char placeholder) {
        super(datePattern, maskPattern, placeholder);

        setBorder(new EmptyBorder(0, 1, 0, 0));

/*        SwingUtils.addFocusTraversalKey(this,
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));*/

    }

    @Override
    public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {

        // не ловим ввод, чтобы его словил сам JTable и обработал
        return ke.getKeyCode() != KeyEvent.VK_ENTER && super.processKeyBinding(ks, ke, condition, pressed);
    }

    //а вот так будем дурить их protected метод
    public boolean publicProcessKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
        return processKeyBinding(ks, ke, condition, pressed);
    }

/*    @Override
    public void focusLost(FocusEvent focusEvent) {
        super.focusLost(focusEvent);
    }*/

}

class BitPropertyEditor extends JCheckBox
                        implements PropertyEditorComponent {

    boolean isNull = false;

    public BitPropertyEditor(Object value) {

        setHorizontalAlignment(JCheckBox.CENTER);
//        setVerticalAlignment(JCheckBox.CENTER);

        setOpaque(true);

        setBackground(Color.white);

        addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON3) {
                    setSelected(false);
                    isNull = true;
                    setBackground(Color.lightGray);
                }
            }
        });

        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                isNull = false;
                setBackground(Color.white);
            }
        });

        if (value != null)
            setSelected((Boolean)value);
    }

    public Component getComponent() {
        return this;
    }

    public Object getCellEditorValue() {

        if (isNull) return null;
        return isSelected();
    }

    public boolean valueChanged() {
        return true;
    }
}

class ObjectPropertyEditor implements PropertyEditorComponent {

    ClientForm clientForm;
    ClientCellView property;

    ClientDialog clientDialog;

    ObjectPropertyEditor(ClientForm iclientForm, ClientCellView iproperty, ClientClass cls, Object value) {

        clientForm = iclientForm;
        property = iproperty;

        clientDialog = new ClientDialog(clientForm, cls, value);
    }

    private boolean objectChosen;
    public Component getComponent() {

        objectChosen = clientDialog.showObjectDialog();

        return null;
    }

    public Object getCellEditorValue() {
        return clientDialog.objectChosen();
    }

    public boolean valueChanged() {
        return objectChosen;
    }
}