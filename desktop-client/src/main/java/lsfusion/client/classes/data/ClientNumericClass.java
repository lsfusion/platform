package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.DoublePropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;

public class ClientNumericClass extends ClientDoubleClass {

    public final static ClientTypeClass type = new ClientTypeClass() {
        public byte getTypeId() {
            return DataType.NUMERIC;
        }

        public ClientNumericClass getDefaultType() {
            return new ClientNumericClass(new ExtInt(10), new ExtInt(2));
        }

        @Override
        public String toString() {
            return ClientResourceBundle.getString("logics.classes.number");
        }
    };
    public final ExtInt precision;
    public final ExtInt scale;

    public ClientNumericClass(ExtInt precision, ExtInt scale) {
        this.precision = precision;
        this.scale = scale;
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

    @Override
    public ClientTypeClass getTypeClass() {
        return type;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        precision.serialize(outStream);
        scale.serialize(outStream);
    }

    public NumberFormat getDefaultFormat() {
        NumberFormat format = super.getDefaultFormat();
        format.setMaximumIntegerDigits(getPrecision() - getScale());
        format.setMaximumFractionDigits(getScale());
        return format;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.number") + (precision.isUnlimited() ? "" : ("[" + precision.value + "," + scale.value + "]"));
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new DoublePropertyEditor(value, property.maxValue, getEditFormat(property), property, BigDecimal.class, property.hasMask());
    }

    @Override
    public Object parseString(String s) throws ParseException {
        try {
            Number n = parseWithDefaultFormat(s);
            return BigDecimal.valueOf(n.doubleValue());
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.big.decimal"), 0);
        }
    }
}
