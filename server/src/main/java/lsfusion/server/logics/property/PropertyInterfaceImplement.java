package lsfusion.server.logics.property;

// вообщем то нужен когда в Object... есть одновременно и calc и action'ы (в If, For и т.п. см. readImplements)
public interface PropertyInterfaceImplement<P extends PropertyInterface> {

    // потому как используется в одном месте
    boolean equalsMap(PropertyInterfaceImplement<P> object);
    int hashMap();
}
