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
import java.awt.event.KeyEvent;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventObject;

public class DateTimePropertyEditor extends JDateChooser implements PropertyEditor {

    public DateTimePropertyEditor(Object value, SimpleDateFormat format, ComponentDesign design) {
        super(null, null, format.toPattern(), new DateTimePropertyEditorComponent(format));

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
        public DateTimePropertyEditorComponent(SimpleDateFormat format) {
            super(format.toPattern(), null, ' ');
            setBorder(new EmptyBorder(0, 1, 0, 0));
        }

        @Override
        public String createMaskFromDatePattern(String datePattern) {
            String symbols = "GyMdkHmsSEDFwWhKzZ";
            String mask = "";
            for (int i = 0; i < datePattern.length(); i++) {
                char ch = datePattern.charAt(i);
                if (ch == 'a') {
                    mask += 'U';
                } else if (symbols.indexOf(ch) != -1) {
                    mask += '#';
                } else {
                    mask += ch;
                }
            }
            return mask;
        }

        @Override
        public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
            // не ловим ESC & Enter, чтобы его словил сам JTable и обработал
            return ke.getKeyCode() != KeyEvent.VK_ESCAPE && ke.getKeyCode() != KeyEvent.VK_ENTER && super.processKeyBinding(ks, ke, condition, pressed);
        }

        //а вот так будем дурить их protected метод
        public boolean publicProcessKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
            return processKeyBinding(ks, ke, condition, pressed);
        }

        public Date getDate() {
            String dateText = getText();
            try {
                return dateFormatter.parse(dateText);
            } catch (ParseException e) {
                //чтобы была возможность не вводить всё значение времени
                //ищем в паттерне символы, которые парсятся в значения time-полей и заменяем их на 1, если они отсутствуют

                if (dateText.isEmpty() || maskPattern.length() != datePattern.length()) {
                    return null;
                }

                int textLength = dateText.length();

                assert textLength == maskPattern.length();

                StringBuilder dateBuilder = new StringBuilder(dateText);

                String hourSymbols = "HhKk";
                String otherTimeSymbols = "sm";

                for (int i = 1; i < textLength - 1; ++i) {
                    char patternCh = datePattern.charAt(i);
                    //благодаря преобразованию через createDateTimeEditFormat, мы точно знаем, что символы в паттерне повторятся
                    if (patternCh == 'S') {
                        ifMatchThenReplace(dateText, dateBuilder, "   ", "111", i);
                        i += 2;
                    } else if (patternCh == 'a') {
                        ifMatchThenReplace(dateText, dateBuilder, "  ", "PM", i);
                        i ++;
                    } else if (hourSymbols.indexOf(patternCh) != -1) {
                        ifMatchThenReplace(dateText, dateBuilder, "  ", "11", i);
                        i ++;
                    } else if (otherTimeSymbols.indexOf(patternCh) != -1) {
                        ifMatchThenReplace(dateText, dateBuilder, "  ", "00", i);
                        i ++;
                    }
                }

                try {
                    return dateFormatter.parse(dateBuilder.toString());
                } catch (ParseException pe) {
                    return null;
                }
            }
        }

        private void ifMatchThenReplace(String src, StringBuilder dest, String match, String replace, int offset) {
            if (src.regionMatches(offset, match, 0, match.length())) {
                dest.replace(offset, offset + match.length(), replace);
            }
        }
    }
}
