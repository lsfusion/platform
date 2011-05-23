package platform.base.identity;

public class DefaultIDGenerator implements IDGenerator {

    int ID = 0;

    public void idRegister(int ID) {
        this.ID = Math.max(this.ID, ID+1);
    }

    public int idShift(int offs) {
        ID += offs;
        return ID;
    }

    public int idShift() {
        return idShift(1);
    }
}
