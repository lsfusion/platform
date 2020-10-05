package lsfusion.client.form.property.cell.classes.controller;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import lsfusion.base.BaseUtils;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;
import lsfusion.client.form.property.table.view.ClientPropertyTableEditorComponent;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;

import static lsfusion.base.DateConverter.*;
import static lsfusion.client.base.view.SwingDefaults.getTableCellMargins;

public class DatePropertyEditor extends JDateChooser implements PropertyEditor, ClientPropertyTableEditorComponent {
    protected final SimpleDateFormat format;

    public DatePropertyEditor(Object value, SimpleDateFormat format, ClientPropertyDraw property) {
        super(null, null, format.toPattern(), new DatePropertyEditorComponent(property, format));
        this.format = format;

        if (value != null) {
            setDate(valueToDate(value));
            ((JFormattedTextField) dateEditor).selectAll();
        }

        if (property != null && property.design != null)
            ClientColorUtils.designComponent(this, property.design);
    }

    public Date valueToDate(Object value) {
        return value instanceof LocalDate ? localDateToSqlDate((LocalDate) value) : null;
    }
    
    public Object dateToValue(Date date) {
        return sqlDateToLocalDate(safeDateToSql(date));
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

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (isInitialized) {
            jcalendar.setFont(null); // не наследуем шрифт от свойства - пусть берёт из Look&Feel'а
        }
    }

    public Object getCellEditorValue() {
        return dateToValue(getDate());
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

    @Override
    public void prepareTextEditor(boolean clearText, boolean selectAll) {
        if (clearText) {
            ((JFormattedTextField) dateEditor).setText("");
        } else if (selectAll) {
            ((JFormattedTextField) dateEditor).selectAll();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(!calendarButton.isVisible())
            return;
        int x = calendarButton.getWidth() - (int) popup.getPreferredSize().getWidth();
        int y = calendarButton.getY() + calendarButton.getHeight();

        Calendar calendar = format.getCalendar();
        Date date = dateEditor.getDate();
        calendar.setTime(date != null ? date : new Date());
        jcalendar.setCalendar(calendar);
        popup.show(calendarButton, x, y);
        dateSelected = false;
    }

    protected static class DatePropertyEditorComponent extends JTextFieldDateEditor {
        public DatePropertyEditorComponent(ClientPropertyDraw property, SimpleDateFormat format) {
            super(format.toPattern(), null, ' ');
            this.dateFormatter = format;

            if (property != null) {
                Integer valueAlignment = property.getSwingValueAlignment();
                if (valueAlignment != null) {
                    setHorizontalAlignment(valueAlignment);
                }
            }

            Insets insets = getTableCellMargins();
            setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right - 1));
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

        @Override
        protected void processKeyEvent(final KeyEvent e) {
            SwingUtils.getAroundTooltipListener(this, e, new Runnable() {
                @Override
                public void run() {
                    DatePropertyEditorComponent.super.processKeyEvent(e);    
                }
            });
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

        // c/p of super method to override colors 
        @Override
        public void caretUpdate(CaretEvent caretEvent) {
            String text = getText().trim();
            String emptyMask = maskPattern.replace('#', placeholder);

            if (text.length() == 0 || text.equals(emptyMask)) {
                setForeground(SwingDefaults.getTableCellForeground());
                return;
            }

            try {
                Date date = dateFormatter.parse(getText());
                if (dateUtil.checkDate(date)) {
                    setForeground(SwingDefaults.getValidDateForeground());
                } else {
                    setForeground(SwingDefaults.getRequiredForeground());
                }
            } catch (Exception e) {
                setForeground(SwingDefaults.getRequiredForeground());
            }
        }

        @Override
        protected void setDate(Date date, boolean b) {
            super.setDate(date, b);

            if (date != null && dateUtil.checkDate(date)) {
                setForeground(SwingDefaults.getTableCellForeground());
            }
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

                String timeSymbols = "HhKksm";

                for (int i = 0; i < textLength - 1; ++i) {
                    char patternCh = datePattern.charAt(i);
                    //благодаря преобразованию через createDateTimeEditFormat, мы точно знаем, что символы в паттерне повторятся
                    if (patternCh == 'S') {
                        ifMatchThenReplace(dateText, dateBuilder, "   ", "000", i);
                        i += 2;
                    } else if (patternCh == 'a') {
                        ifMatchThenReplace(dateText, dateBuilder, "  ", "PM", i);
                        i ++;
                    } else if (patternCh == 'y') {
                        int symbolCount = BaseUtils.countRepeatingChars(datePattern, 'y', i);
                        String yearPattern = BaseUtils.replicate('y', symbolCount);
                        String currentYear = new SimpleDateFormat(yearPattern).format(Calendar.getInstance().getTime());
                        String spaces = BaseUtils.spaces(symbolCount);
                        ifMatchThenReplace(dateText, dateBuilder, spaces, currentYear, i);
                        i++;
                    } else if (timeSymbols.indexOf(patternCh) != -1 && dateText.matches(".*\\d+.*")) { // если изначально не содержит цифру - сбрасываем в null
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
