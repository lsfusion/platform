package lsfusion.base.identity;

public interface IDGenerator {

    void idRegister(int ID);
    int idShift(int offs);
    int idShift();
}
