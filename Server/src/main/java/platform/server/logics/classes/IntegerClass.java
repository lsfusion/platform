package platform.server.logics.classes;

public class IntegerClass extends IntegralClass {
    IntegerClass(Integer iID, String caption) {super(iID, caption);}

    public Class getJavaClass() {
        return Integer.class;
    }

    public byte getTypeID() {
        return 1;
    }
}
