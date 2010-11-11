package platform.client.logics.classes;

public interface ClientTypeClass {

    byte getTypeId();

    ClientClass getDefaultClass(ClientObjectClass baseClass);

    ClientType getDefaultType();
}
