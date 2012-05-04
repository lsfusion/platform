package platform.client.form.editor;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import platform.base.DateConverter;
import platform.client.SwingUtils;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.cell.PropertyTableCellEditor;
import platform.interop.ComponentDesign;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventObject;

public class DateTimePropertyEditor extends JDateChooser implements PropertyEditorComponent {

    public DateTimePropertyEditor(Object value, SimpleDateFormat format, ComponentDesign design) {
        super(null, null, format.toPattern(), new DateTimePropertyEditorComponent(format.toPattern(), "##.##.##", ' '));

        if (value != null) {
            setDate(DateConverter.stampToDate((Timestamp) value));
        }
        ((JFormattedTextField) dateEditor).selectAll();

        if (design != null) {
            design.designCell(this);
        }
    }

    @Override
    public boolean requestFocusInWindow() {
        // пересылаем фокус в нужный объект
        return ((JFormattedTextField) dateEditor).requestFocusInWindow();
    }

    @Override
    public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
        // передаем вниз нажатую клавишу, чтобы по нажатию кнопки она уже начинала вводить в объект
        if (condition == WHEN_FOCUSED) {
            return ((DateTimePropertyEditorComponent) dateEditor).publicProcessKeyBinding(ks, ke, condition, pressed);
        } else {
            return super.processKeyBinding(ks, ke, condition, pressed);
        }
    }

    @Override
    public void setNextFocusableComponent(Component comp) {
        super.setNextFocusableComponent(comp);
        ((JComponent) dateEditor).setNextFocusableComponent(comp);
//        jcalendar.setNextFocusableComponent(dateEditor.getUiComponent());


        // вот эту хрень приходится добавлять по той причине, что иначе так как popup вообще говоря не child таблицы,
        // то при нажатии на что угодно - она тут же делает stopEditing...
        if (comp instanceof JTable) {
            final JTable table = (JTable) comp;

            popup.addPopupMenuListener(new PopupMenuListener() {

                Boolean oldValue;

                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    oldValue = (Boolean) table.getClientProperty("terminateEditOnFocusLost");
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

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        //пока не нужен
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return this;
    }

    public Object getCellEditorValue() {
        return DateConverter.dateToStamp(dateEditor.getDate());
    }

    @Override
    public boolean stopCellEditing() {
        return true;
    }

    private static class DateTimePropertyEditorComponent extends JTextFieldDateEditor {
        public DateTimePropertyEditorComponent(String datePattern, String maskPattern, char placeholder) {
            super(datePattern, maskPattern, placeholder);

            setBorder(new EmptyBorder(0, 1, 0, 0));
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

        public Date getDate() {
            try {
                String dateText = getText();
                if (dateText.endsWith("  :  :  ")) {
                    dateText = dateText.substring(0, dateText.length() - 8) + "12:00:00";
                }
                return dateFormatter.parse(dateText);
            } catch (ParseException e) {
                return null;
            }
        }
    }
}
