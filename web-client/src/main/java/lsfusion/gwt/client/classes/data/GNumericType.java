package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.LocaleInfo;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.NumericGridCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.NumberGridCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

import java.math.BigDecimal;
import java.text.ParseException;

import static lsfusion.gwt.client.base.GwtSharedUtils.countMatches;

public class GNumericType extends GDoubleType {
    private GExtInt length;
    private GExtInt precision;

    @SuppressWarnings("unused")
    public GNumericType() {}

    public GNumericType(GExtInt length, GExtInt precision) {
        this.length = length;
        this.precision = precision;
        defaultPattern = getPattern();
    }

    @Override
    public AbstractGridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new NumberGridCellRenderer(property);
    }

    @Override
    protected int getLength() {
        //as in server Settings
        return length.isUnlimited() ? 127 : length.value;
    }

    protected int getPrecision() {
        //as in server Settings
        return precision.isUnlimited() ? 32 : precision.value;
    }

    private String getPattern() {
        String pattern = "#,###";
        if (getPrecision() > 0) {
            pattern += ".";
            
            for (int i = 0; i < getPrecision(); i++) {
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

        if (!length.isUnlimited() && ((precision.value == 0 && s.contains(decimalSeparator)) ||
                (s.contains(decimalSeparator) && s.length() - s.indexOf(decimalSeparator) > precision.value + 1))) {
            throwParseException(s);
        }
        
        String groupingSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().groupingSeparator();
        if (UNBREAKABLE_SPACE.equals(groupingSeparator)) {
            groupingSeparator = " ";
        }
        int allowedSeparatorPosition = getLength() - getPrecision() + countMatches(s, "-") + countMatches(s, groupingSeparator);
        int separatorPosition = s.contains(decimalSeparator) ? s.indexOf(decimalSeparator) : s.length();
        if (separatorPosition > allowedSeparatorPosition) {
            throwParseException(s);
        }
        
        return BigDecimal.valueOf(toDouble);
    }
    
    private void throwParseException(String s) throws ParseException {
        throw new ParseException("String " + s + "can not be converted to numeric" + (length.isUnlimited() ? "" : ("[" + length + "," + precision + "]")), 0);
    } 

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeNumericCaption() + (length.isUnlimited() ? "" : ("[" + length + "," + precision + "]"));
    }
}
