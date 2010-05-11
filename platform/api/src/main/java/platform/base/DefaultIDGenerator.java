package platform.base;

public class DefaultIDGenerator implements IDGenerator {

    int ID = 0;
    public int genID() {
        return ID++;
    }
}
