package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.LocaleInfo;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.GNumericDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.NumericCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.IntegralCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

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
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new IntegralCellRenderer(property);
    }

    @Override
    protected int getPrecision() {
        //as in server Settings
        return precision.isUnlimited() ? 127 : precision.value;
    }

    @Override
    public PValue fromDoubleValue(double doubleValue) {
        return PValue.getPValue(new GNumericDTO(doubleValue));
    }
    @Override
    public double getDoubleValue(PValue value) {
        return PValue.getNumericValue(value).value;
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
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new NumericCellEditor(this, editManager, editProperty);
    }

    @Override
    public PValue parseString(String s, String pattern) throws ParseException {
        PValue parsed = super.parseString(s, pattern);
        if(parsed != null)
            checkParse(s);

        return parsed;
    }

    private void checkParse(String s) throws ParseException {
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
    }

    private void throwParseException(String s) throws ParseException {
        throw new ParseException("String " + s + "can not be converted to numeric" + (precision.isUnlimited() ? "" : ("[" + precision + "," + scale + "]")), 0);
    } 

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeNumericCaption() + (precision.isUnlimited() ? "" : ("[" + precision + "," + scale + "]"));
    }

    @Override
    public boolean isId() {
        return this.scale.getValue() == 0;
    }
}
