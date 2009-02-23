package platform.server.logics.properties;

import platform.server.logics.classes.DataClass;

public class DataPropertyInterface extends PropertyInterface<DataPropertyInterface> {
    public DataClass interfaceClass;

    public DataPropertyInterface(int iID, DataClass iClass) {
        super(iID);
        interfaceClass = iClass;
    }
}
