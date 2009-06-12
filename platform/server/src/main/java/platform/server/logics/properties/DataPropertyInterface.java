package platform.server.logics.properties;

import platform.server.data.classes.ValueClass;

public class DataPropertyInterface extends PropertyInterface<DataPropertyInterface> {
    public ValueClass interfaceClass;

    public DataPropertyInterface(int iID, ValueClass iClass) {
        super(iID);
        interfaceClass = iClass;
    }
}
