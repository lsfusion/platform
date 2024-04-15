package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.property.ClientPropertyDraw;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

public class DoublePropertyEditor extends TextFieldPropertyEditor {
    DecimalFormat df = null;
    boolean hasMask = false;
    public DoublePropertyEditor(Object value, Long maxValue, NumberFormat format, ClientPropertyDraw property, Class formatterValueClass, final boolean hasMask) {
        super(property);
        df = format != null ? (DecimalFormat) format : new DecimalFormat();
        this.hasMask = hasMask;
        final boolean isGroupSeparatorDot = df.getDecimalFormatSymbols().getGroupingSeparator() == '.';
        final char separator = df.getDecimalFormatSymbols().getDecimalSeparator();
        final char groupingSeparator = df.getDecimalFormatSymbols().getGroupingSeparator();

        NumberFormatter formatter = new NullNumberFormatter(df, isGroupSeparatorDot ? 0 : 0.0, String.valueOf(separator)) {
            public boolean lastTextEndsWithSeparator;
            public int lastZero;

            @Override
            public String valueToString(Object value) throws ParseException {
                String result = super.valueToString(value);
                if (lastTextEndsWithSeparator && result != null && result.indexOf(separator) == -1) {
                    result += separator;
                    lastTextEndsWithSeparator = false;
                }
                if (minusZeroText == null) {
                    int maxFractionDigits = df.getMaximumFractionDigits();
                    int currentFractionDigits = countFractionDigits(result, false);
                    if(hasMask && result != null) {
                        while (maxFractionDigits < currentFractionDigits) {
                            result = result.substring(0, result.length() - 1);
                            currentFractionDigits--;
                        }
                    }
                    int currentFractionZeroDigits = countFractionDigits(result, true);
                    while(lastZero > currentFractionZeroDigits) {
                        result += '0';
                        currentFractionZeroDigits++;
                    }
                }
                return result;
            }

            @Override
            public Object stringToValue(String text) throws ParseException {
                text = BaseUtils.replaceSeparators(text, separator, groupingSeparator);
                lastZero = 0;
                if (text != null && text.length() > 0) {
                    //если >1 decimalSeparator, удаляем по предпоследний включительно
                    while(text.indexOf(separator) != text.lastIndexOf(separator)) {
                        text = text.substring(0, text.lastIndexOf(separator));
                    }
                    if (text.indexOf(separator) != -1) {
                        while (lastZero < text.length() - 1 && text.charAt(text.length() - 1 - lastZero) == '0') {
                            lastZero++;
                        }
                    }
                    lastTextEndsWithSeparator = text.indexOf(separator) == text.length() - 1 - lastZero;
                } else {
                    lastTextEndsWithSeparator = false;
                }
                return super.stringToValue(text);
            }

            private int countFractionDigits(String text, boolean onlyZero) {
                int count = 0;
                if(text.indexOf(separator) > -1) {
                    while (!text.endsWith(String.valueOf(separator)) &&(!onlyZero ||text.endsWith("0"))) {
                        text = text.substring(0, text.length() - 1);
                        count++;
                    }
                }
                return count;
            }
        };

        //через reflection добавляем к разрешённым символам второй decimal separator (. или ,)
        try {
            Field field = NumberFormatter.class.getDeclaredField("specialChars");
            field.setAccessible(true);
            String specialChars = (String) field.get(formatter);
            if(!specialChars.contains("."))
                specialChars +=".";
            if(!specialChars.contains(","))
                specialChars +=",";
            field.set(formatter, specialChars);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        formatter.setValueClass(formatterValueClass);
        formatter.setAllowsInvalid(false);
        if (maxValue != null)
            formatter.setMaximum(new BigDecimal(maxValue));

        setFormatterFactory(new DefaultFormatterFactory(formatter));

        if (value != null) {
            try {
                setText(formatter.valueToString(value));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }


        //we catch 'backspace' and 'delete' but not 'del', so 'del' is not handled correctly
        ActionMap actionMap = getActionMap();
        actionMap.put("delete-previous", new MoveCaretAction(actionMap.get("delete-previous"), hasMask, false));
        actionMap.put("delete-next", new MoveCaretAction(actionMap.get("delete-next"), hasMask, true));

    }

    //курсор перескакивает через decimalSeparator при удалении
    class MoveCaretAction extends AbstractAction {
        Action defaultAction;
        boolean hasMask;
        boolean forward;
        public MoveCaretAction(Action defaultAction, boolean hasMask, boolean forward) {
            this.defaultAction = defaultAction;
            this.hasMask = hasMask;
            this.forward = forward;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (hasMask) {
                //проверяем, не справа(backspace)/слева(delete) ли мы от decimalSeparator. Если да, смещаемся влево(backspace)/вправо(delete) и игнорируем событие
                //иначе событие происходит
                if (!moveCaret()) {
                    if (defaultAction != null) {
                        defaultAction.actionPerformed(e);
                    }
                    moveCaretAfterZero();
                }
            } else {
                //проверяем, не пытаемся ли мы удалить decimalSeparator. Если да, то проверяем, не наступит ли в результате удаления переполнения.
                if (!checkOverflow()) {
                    if (defaultAction != null)
                        defaultAction.actionPerformed(e);
                }
            }
        }

        private boolean moveCaret() {
            String text = getText();
            if (text != null) {
                int currentPosition = getCaret().getDot();
                char separator = df.getDecimalFormatSymbols().getDecimalSeparator();
                int separatorPosition = text.indexOf(separator);
                if (separatorPosition >= 0 && separatorPosition == currentPosition + (forward ? 0 : -1)) {
                    getCaret().setDot(currentPosition + (forward ? 1 : -1));
                    return true;
                }
            }
            return false;
        }

        private boolean checkOverflow() {
            String text = getText();
            if (text != null) {
                int currentPosition = getCaret().getDot();
                char separator = df.getDecimalFormatSymbols().getDecimalSeparator();
                char groupingSeparator = df.getDecimalFormatSymbols().getGroupingSeparator();
                int separatorPosition = text.indexOf(separator);
                if (separatorPosition >= 0 && separatorPosition == currentPosition + (forward ? 0 : -1)) {
                    int length = text.replace(String.valueOf(groupingSeparator), "").replace(String.valueOf(separator), "").replace("-", "").length();
                    if (length > df.getMaximumIntegerDigits()) {
                        return true;
                    }
                }
            }
            return false;
        }

        //if after deleting a character the value of the integer part is "0" and the cursor is in front of "0", move it to the right.
        private void moveCaretAfterZero() {
            if (leftIsZero(getText()) && getCaretPosition() == 0)
                setCaret(1);
        }
    }

    @Override
    public void replaceSelection(String content) {
        boolean ignore = false;
        //при переходе из minusZeroText(-, -0, -0.00 и т.д.) в обычное значение добавляются нули маски, и каретка перемещается на лишний символ вправо
        //та же проблема с кареткой при полной замене отрицательного значения на положительное (минус сохраняется, и каретка смещается)
        boolean moveCaretBack = false;
        char separator = df.getDecimalFormatSymbols().getDecimalSeparator();
        char groupingSeparator = df.getDecimalFormatSymbols().getGroupingSeparator();
        content = BaseUtils.replaceSeparators(content, separator, groupingSeparator);
        if (getSelectedText() == null) {
            String text = getText();
            if(isMinusZeroText(text, separator) && hasMask)
                moveCaretBack = true;
            if (content.equals("-")) {
                if (text.startsWith("-")) {
                    setSingleSelection(0);
                    content = "";
                } else {
                    setText("-" + text);
                    ignore = true;
                }
            } else {
                int currentPosition = getCaret().getDot();
                int separatorPosition = text.indexOf(separator);
                if(content.equals(String.valueOf(separator))) {
                    //если ставится decimalSeparator на месте decimalSeparator, игнорируем.
                    if (currentPosition == separatorPosition) {
                        setCaret(currentPosition + 1);
                        ignore = true;
                    }
                } else if (StringUtils.isNumeric(content)) {
                    //проверяем, не превышен ли лимит символов до/после запятой
                    String[] split = text.replace(String.valueOf(df.getDecimalFormatSymbols().getGroupingSeparator()), "").replace("-", "").split(String.valueOf('\\') + String.valueOf(separator));
                    if (currentPosition <= separatorPosition || separatorPosition == -1) {
                        if (split.length > 0 && df.getMaximumIntegerDigits() <= split[0].length())
                            setSingleSelection(currentPosition - (currentPosition == separatorPosition || currentPosition == text.length() ? 1 : 0));
                    } else {
                        if (split.length > 1 && df.getMaximumFractionDigits() <= split[1].length())
                            setSingleSelection(currentPosition - (currentPosition == text.length() ? 1 : 0));
                    }
                }
            }
        } else {
            String text = getText();
            if (text != null && getSelectedText().equals(text) && text.startsWith("-") && hasMask)
                moveCaretBack = true;
        }
        if(!ignore) {
            //after replacing zero with another digit, the cursor moves over the separator, move it to the left
            boolean prevLeftIsZero = leftIsZero(getText()) && getCaretPosition() == 1 && hasMask;
            if(prevLeftIsZero)
                moveCaretBack = true;
            super.replaceSelection(content);
        }
        if(moveCaretBack && !isMinusZeroText(getText(), separator))
            setCaret(getCaretPosition() - 1);
    }

    private boolean leftIsZero(String text) {
        if(text != null) {
            int separatorPosition = text.indexOf(df.getDecimalFormatSymbols().getDecimalSeparator());
            if (separatorPosition >= 0) {
                String left = text.substring(0, separatorPosition).replace(String.valueOf(df.getDecimalFormatSymbols().getGroupingSeparator()), "");
                return left.equals("0");
            }
        }
        return false;
    }

    private void setSingleSelection(int start) {
        setSelectionStart(start);
        setSelectionEnd(start + 1);
    }

    private void setCaret(int position) {
        if(position >= 0)
            setCaretPosition(position);
    }

    private boolean isMinusZeroText(String text, char decimalSeparator) {
        //as in NullNumberFormatter
        return text.equals("-") ||
                text.equals("-0") ||
                text.equals("-0" + decimalSeparator) ||
                text.equals("-0" + decimalSeparator + "0") ||
                text.equals("-0" + decimalSeparator + "00") ||
                text.equals("-0" + decimalSeparator + "000");
    }
}
