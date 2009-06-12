package platform.client.logics.classes;

import java.io.DataInputStream;
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

    public Format getDefaultFormat() {
        NumberFormat format = (NumberFormat)super.getDefaultFormat();

        format.setMaximumIntegerDigits(length - precision - ((precision > 0)?1:0));
        format.setMaximumFractionDigits(precision);
        return format;
    }
}
