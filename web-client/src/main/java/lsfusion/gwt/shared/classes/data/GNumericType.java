package lsfusion.gwt.shared.classes.data;

import com.google.gwt.i18n.client.LocaleInfo;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.NumericGridCellEditor;
import lsfusion.gwt.client.form.property.cell.GridCellRenderer;
import lsfusion.gwt.client.form.property.cell.classes.NumberGridCellRenderer;
import lsfusion.gwt.shared.form.property.GPropertyDraw;

import java.math.BigDecimal;
import java.text.ParseException;

import static lsfusion.gwt.shared.GwtSharedUtils.countMatches;

public class GNumericType extends GDoubleType {
    private int length = 10;
    private int precision = 2;

    public GNumericType() {}

    public GNumericType(int length, int precision) {
        this.length = length;
        this.precision = precision;
        defaultPattern = getPattern();
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new NumberGridCellRenderer(property);
    }

    @Override
    protected int getLength() {
        return length;
    }

    private String getPattern() {
        String pattern = "#,###";
        if (precision > 0) {
            pattern += ".";
            
            for (int i = 0; i < precision; i++) {
                pattern += "#";
            }
        }
        return pattern;
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new NumericGridCellEditor(this, editManager, editProperty, getEditFormat(editProperty));
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        Double toDouble = parseToDouble(s, pattern); // сперва проверим, конвертится ли строка в число вообще

        String decimalSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().decimalSeparator(); // а затем посчитаем цифры
        
        if ((precision == 0 && s.contains(decimalSeparator)) ||
                (s.contains(decimalSeparator) && s.length() - s.indexOf(decimalSeparator) > precision + 1)) {
            throwParseException(s);
        }
        
        String groupingSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().groupingSeparator();
        if (UNBREAKABLE_SPACE.equals(groupingSeparator)) {
            groupingSeparator = " ";
        }
        int allowedSeparatorPosition = length - precision + countMatches(s, "-") + countMatches(s, groupingSeparator);
        int separatorPosition = s.contains(decimalSeparator) ? s.indexOf(decimalSeparator) : s.length();
        if (separatorPosition > allowedSeparatorPosition) {
            throwParseException(s);
        }
        
        return BigDecimal.valueOf(toDouble);
    }
    
    private void throwParseException(String s) throws ParseException {
        throw new ParseException("String " + s + "can not be converted to numeric[" + length + "," + precision + "]", 0);   
    } 

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeNumericCaption() + '[' + length + ',' + precision + ']';
    }
}
