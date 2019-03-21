package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.DoublePropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.interop.classes.DataType;

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
            return new ClientNumericClass(10, 2);
        }

        @Override
        public String toString() {
            return ClientResourceBundle.getString("logics.classes.number");
        }
    };

    @Override
    protected int getLength() {
        return length;
    }

    public final int length;
    public final int precision;

    public ClientNumericClass(int length, int precision) {
        this.length = length;
        this.precision = precision;
    }

    @Override
    public ClientTypeClass getTypeClass() {
        return type;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(length);
        outStream.writeInt(precision);
    }

    public NumberFormat getDefaultFormat() {
        NumberFormat format = super.getDefaultFormat();
        format.setMaximumIntegerDigits(length - precision);
        format.setMaximumFractionDigits(precision);
        return format;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.number") + '[' + length + ',' + precision + ']';
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new DoublePropertyEditor(value, property.maxValue, getEditFormat(property), property.design, BigDecimal.class, property.hasMask());
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
