package platform.base;

public class DefaultIDGenerator implements IDGenerator {

    int ID = 0;

    public int idShift(int offs) {
        ID += offs;
        return ID;
    }

    public int idShift() {
        return idShift(1);
    }
}
