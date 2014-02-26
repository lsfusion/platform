package lsfusion.gwt.form.shared.view.classes;

import com.google.gwt.i18n.client.LocaleInfo;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.NumericGridCellEditor;

import java.math.BigDecimal;
import java.text.ParseException;

public class GNumericType extends GDoubleType {
    private int length = 10;
    private int precision = 2;

    public GNumericType() {}

    public GNumericType(int length, int precision) {
        this.length = length;
        this.precision = precision;
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new NumericGridCellEditor(this, editManager, editProperty);
    }

    @Override
    public Object parseString(String s) throws ParseException {
        Double toDouble = parseToDouble(s); // сперва проверим, конвертится ли строка в число вообще 

        String decimalSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().decimalSeparator(); // а затем посчитаем цифры
        
        if ((precision == 0 && s.contains(decimalSeparator)) ||
                (s.contains(decimalSeparator) && s.length() - s.indexOf(decimalSeparator) > precision + 1)) {
            throwParseException(s);
        }
        
        int separatorPosition = s.contains(decimalSeparator) ? s.indexOf(decimalSeparator) : s.length();
        int allowedIntegralLength = length - precision + s.lastIndexOf('-') + 1;
        if (separatorPosition > allowedIntegralLength) {
            throwParseException(s);
        }
        return BigDecimal.valueOf(toDouble);
    }
    
    private void throwParseException(String s) throws ParseException {
        throw new ParseException("String " + s + "can not be converted to numeric[" + length + "," + precision + "]", 0);   
    } 

    @Override
    public String toString() {
        return "Число" + '[' + length + ',' + precision + ']';
    }
}
