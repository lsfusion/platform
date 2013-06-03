package lsfusion.client.form.editor;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import lsfusion.base.DateConverter;
import lsfusion.client.SwingUtils;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.cell.PropertyTableCellEditor;
import lsfusion.interop.ComponentDesign;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;

public class DatePropertyEditor extends JDateChooser implements PropertyEditor {
    SimpleDateFormat format;

    public DatePropertyEditor(Object value, SimpleDateFormat format, ComponentDesign design) {
        super(null, null, format.toPattern(), new DatePropertyEditorComponent(format, "##.##.##", ' '));
        this.format = format;

        if (value != null) {
            setDate(DateConverter.sqlToDate((java.sql.Date) value));
            ((JFormattedTextField) dateEditor).selectAll();
        }

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
            return ((DatePropertyEditorComponent) dateEditor).publicProcessKeyBinding(ks, ke, condition, pressed);
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
        return DateConverter.safeDateToSql(getDate());
    }

    @Override
    public boolean stopCellEditing() {
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int x = calendarButton.getWidth()
                - (int) popup.getPreferredSize().getWidth();
        int y = calendarButton.getY() + calendarButton.getHeight();

        Calendar calendar = format.getCalendar();
        Date date = dateEditor.getDate();
        calendar.setTime(date != null ? date : new Date());
        jcalendar.setCalendar(calendar);
        popup.show(calendarButton, x, y);
        dateSelected = false;
    }

    private static class DatePropertyEditorComponent extends JTextFieldDateEditor {

        public DatePropertyEditorComponent(SimpleDateFormat format, String maskPattern, char placeholder) {
            super(format.toPattern(), maskPattern, placeholder);
            this.dateFormatter = format;
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
                return dateFormatter.parse(getText());
            } catch (ParseException e) {
                return null;
            }
        }

/*    @Override
    public void focusLost(FocusEvent focusEvent) {
        super.focusLost(focusEvent);
    }*/

    }
}
