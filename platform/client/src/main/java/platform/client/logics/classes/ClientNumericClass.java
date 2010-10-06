package platform.client.logics.classes;

import platform.interop.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;

public class ClientNumericClass extends ClientDoubleClass {

    private int length;
    private int precision;

    public ClientNumericClass(DataInputStream inStream) throws IOException {
        super(inStream);

        length = inStream.readInt();
        precision = inStream.readInt();
    }

    @Override
    public byte getTypeId() {
        return Data.NUMERIC;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(length);
        outStream.writeInt(precision);
    }

    public Format getDefaultFormat() {
        NumberFormat format = (NumberFormat)super.getDefaultFormat();

        format.setMaximumIntegerDigits(length - precision - ((precision > 0)?1:0));
        format.setMaximumFractionDigits(precision);
        return format;
    }
}
