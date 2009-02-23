package platform.client.form;

import com.toedter.calendar.JDateChooser;
import platform.client.form.PropertyEditorComponent;
import platform.client.SwingUtils;

import java.text.SimpleDateFormat;
import java.awt.event.KeyEvent;
import java.awt.*;

import platform.base.DateConverter;

import javax.swing.*;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;

public class DatePropertyEditor extends JDateChooser
                           implements PropertyEditorComponent {

    public DatePropertyEditor(Object value, SimpleDateFormat format) {
        super(null, null, format.toPattern(), new DatePropertyEditorComponent(format.toPattern(),"##.##.##",' '));

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
