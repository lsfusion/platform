package lsfusion.base.identity;

public class DefaultIDGenerator implements IDGenerator {

    private int ID = 0;

    public synchronized void idRegister(int ID) {
        this.ID = Math.max(this.ID, ID+1);
    }

    public synchronized int idShift(int offs) {
        ID += offs;
        return ID;
    }

    public synchronized int idShift() {
        return idShift(1);
    }
}
