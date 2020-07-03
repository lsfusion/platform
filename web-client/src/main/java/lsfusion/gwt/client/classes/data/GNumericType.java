package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.LocaleInfo;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.NumericCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.NumberCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.math.BigDecimal;
import java.text.ParseException;

import static lsfusion.gwt.client.base.GwtSharedUtils.countMatches;

public class GNumericType extends GDoubleType {
    private GExtInt precision;
    private GExtInt scale;

    @SuppressWarnings("unused")
    public GNumericType() {}

    public GNumericType(GExtInt precision, GExtInt scale) {
        this.precision = precision;
        this.scale = scale;
        defaultPattern = getPattern();
    }

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new NumberCellRenderer(property);
    }

    @Override
    protected int getPrecision() {
        //as in server Settings
        return precision.isUnlimited() ? 127 : precision.value;
    }

    protected int getScale() {
        //as in server Settings
        return scale.isUnlimited() ? 32 : scale.value;
    }

    private String getPattern() {
        String pattern = "#,###";
        if (getScale() > 0) {
            pattern += ".";
            
            for (int i = 0; i < getScale(); i++) {
                pattern += "#";
            }
        }
        return pattern;
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new NumericCellEditor(this, editManager, editProperty, getEditFormat(editProperty));
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        Double toDouble = parseToDouble(s, pattern); // сперва проверим, конвертится ли строка в число вообще

        String decimalSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().decimalSeparator(); // а затем посчитаем цифры

        if (!precision.isUnlimited() && ((scale.value == 0 && s.contains(decimalSeparator)) ||
                (s.contains(decimalSeparator) && s.length() - s.indexOf(decimalSeparator) > scale.value + 1))) {
            throwParseException(s);
        }
        
        String groupingSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().groupingSeparator();
        if (UNBREAKABLE_SPACE.equals(groupingSeparator)) {
            groupingSeparator = " ";
        }
        int allowedSeparatorPosition = getPrecision() - getScale() + countMatches(s, "-") + countMatches(s, groupingSeparator);
        int separatorPosition = s.contains(decimalSeparator) ? s.indexOf(decimalSeparator) : s.length();
        if (separatorPosition > allowedSeparatorPosition) {
            throwParseException(s);
        }
        
        return BigDecimal.valueOf(toDouble);
    }
    
    private void throwParseException(String s) throws ParseException {
        throw new ParseException("String " + s + "can not be converted to numeric" + (precision.isUnlimited() ? "" : ("[" + precision + "," + scale + "]")), 0);
    } 

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeNumericCaption() + (precision.isUnlimited() ? "" : ("[" + precision + "," + scale + "]"));
    }
}
