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

public interface PropertyEditorComponent {

    Component getComponent();

    void setCellEditorValue(Object value);
    Object getCellEditorValue();

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

    public IntegerPropertyEditor(NumberFormat iformat) {

        NumberFormat format = iformat;
        if (format == null)
            format = NumberFormat.getInstance();

/*        if (format instanceof DecimalFormat) {
            ((DecimalFormat) format).setDecimalSeparatorAlwaysShown(true);
        }*/

        NumberFormatter formatter = new NumberFormatter(format) {

            public Object stringToValue(String text) throws ParseException {
                if (text.isEmpty() || text.equals("-") || text.equals(",") || text.equals(".")) return null;
                return super.stringToValue(text);
            }
        };

        formatter.setAllowsInvalid(false);

        this.setHorizontalAlignment(JTextField.RIGHT);

        setFormatterFactory(new DefaultFormatterFactory(formatter));

    }

    public Component getComponent() {
        return this;
    }

    public void setCellEditorValue(Object value) {
        if (value != null)
            setValue(value);
        selectAll();
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

}

class StringPropertyEditor extends TextFieldPropertyEditor
                           implements PropertyEditorComponent {

    public StringPropertyEditor() {
        super();
    }

    public Component getComponent() {
        return this;
    }

    public void setCellEditorValue(Object value) {
        if (value != null)
            setText(value.toString());
        selectAll();
    }

    public Object getCellEditorValue() {
        if (getText().isEmpty()) return null;
        return getText();
    }

}

class DatePropertyEditor extends JDateChooser
                           implements PropertyEditorComponent {

    public DatePropertyEditor() {
        super(null, null, "dd.MM.yy", new DatePropertyEditorComponent("dd.MM.yy","##.##.##",' '));

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

    public void setCellEditorValue(Object value) {
        if (value != null)
            setDate(DateConverter.intToDate((Integer)value));
        ((JFormattedTextField)dateEditor).selectAll();
    }

    public Object getCellEditorValue() {

        return DateConverter.dateToInt(getDate());
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

    public BitPropertyEditor() {

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
    }

    public Component getComponent() {
        return this;
    }

    public void setCellEditorValue(Object value) {
        if (value != null)
            setSelected((Boolean)value);
    }

    public Object getCellEditorValue() {

        if (isNull) return null;
        return isSelected();
    }
}

class ObjectPropertyEditor implements PropertyEditorComponent {

    ClientDialog clientDialog;

    private boolean objectChosen;

    private Object oldValue;

    ObjectPropertyEditor(ClientForm clientForm) {

        clientDialog = new ClientDialog(clientForm);
    }

    public Component getComponent() {

        objectChosen = clientDialog.showObjectDialog();
        return null;
    }

    public void setCellEditorValue(Object value) {

        oldValue = value;
        clientDialog.createDefaultForm((Integer)value);
    }

    public Object getCellEditorValue() {
        if (objectChosen)
            return clientDialog.objectChosen();
        else
            return oldValue;
    }
}