package platform.server.logics.properties;

import platform.server.data.classes.CustomClass;
import platform.server.data.classes.ValueClass;

public class ClassPropertyInterface extends PropertyInterface<ClassPropertyInterface> {
    public ValueClass interfaceClass;

    public ClassPropertyInterface(int iID, ValueClass iClass) {
        super(iID);
        interfaceClass = iClass;
    }
}
