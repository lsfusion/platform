package platform.server.logics.classes;

import platform.server.data.types.Type;

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
