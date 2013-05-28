package platform.base.identity;

public interface IDGenerator {

    public void idRegister(int ID);
    public int idShift(int offs);
    public int idShift();
}
