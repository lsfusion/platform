package platform.server.logics.classes;

import platform.server.data.types.Type;

public class DoubleClass extends IntegralClass {
    DoubleClass(Integer iID, String caption) {super(iID, caption);}

    public Type getType() {
        return Type.doubleType;
    }

    public Class getJavaClass() {
        return Double.class;
    }

    public byte getTypeID() {
        return 3;
    }
}
