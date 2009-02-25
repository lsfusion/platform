package platform.server.logics.classes;

import platform.server.data.types.Type;
import platform.client.interop.classes.ClientClass;
import platform.client.interop.classes.ClientDoubleClass;

class NumericClass extends DoubleClass {

    int length;
    int precision;

    NumericClass(Integer iID, String caption, int iLength, int iPrecision) {
        super(iID, caption);
        length = iLength;
        precision = iPrecision;
    }

    public Type getType() {
        return Type.numeric(length,precision);
    }
}
