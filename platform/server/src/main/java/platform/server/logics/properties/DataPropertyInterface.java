package platform.server.logics.properties;

import platform.server.logics.classes.RemoteClass;

public class DataPropertyInterface extends PropertyInterface<DataPropertyInterface> {
    public RemoteClass interfaceClass;

    public DataPropertyInterface(int iID, RemoteClass iClass) {
        super(iID);
        interfaceClass = iClass;
    }
}
