package platform.server.logics.classes;

import platform.server.data.types.Type;

import java.io.DataOutputStream;
import java.io.IOException;

class NumericClass extends DoubleClass {

    byte length;
    byte precision;

    NumericClass(Integer iID, String caption, byte iLength, byte iPrecision) {
        super(iID, caption);
        length = iLength;
        precision = iPrecision;
    }

    public byte getTypeID() {
        return 4; 
    }

    public Type getType() {
        return Type.numeric(length,precision);
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        super.serialize(outStream);

        outStream.writeByte(length);
        outStream.writeByte(precision);
    }
}
