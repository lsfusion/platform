package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.IntegerPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.classes.DataType;

import java.text.NumberFormat;
import java.text.ParseException;

public class ClientIntegerClass extends ClientIntegralClass implements ClientTypeClass {

    public final static ClientIntegerClass instance = new ClientIntegerClass();

    public byte getTypeId() {
        return DataType.INTEGER;
    }

    @Override
    protected int getPrecision() {
        return 8;
    }

    @Override
    public NumberFormat getDefaultFormat() {
        NumberFormat format = super.getDefaultFormat();
        format.setParseIntegerOnly(true);
        return format;
    }

    @Override
    public Object parseString(String s) throws ParseException {
        try {
            Number n = parseWithDefaultFormat(s);
            long val = n.longValue();
            if (val > (long)Integer.MAX_VALUE) {
                throw new NumberFormatException("Integer " + val + " is out of range");
            }
            return (int)val;
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.double"), 0);
        }
    }

    @Override
    public PropertyEditor getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, AsyncChangeInterface asyncChange, Object value) {
        return new IntegerPropertyEditor(value, getEditFormat(property), property, Integer.class);
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new IntegerPropertyEditor(value, maxValue(property.maxValue), getEditFormat(property), property, Integer.class);
    }

    private Integer maxValue(Long maxValue) {
        return maxValue == null
               ? null
               : maxValue > Integer.MAX_VALUE ? Integer.MAX_VALUE : maxValue.intValue();
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.integer");
    }
}
