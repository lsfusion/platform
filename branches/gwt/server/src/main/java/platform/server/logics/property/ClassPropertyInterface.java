package platform.server.logics.property;

import platform.server.classes.ValueClass;

public class ClassPropertyInterface extends PropertyInterface<ClassPropertyInterface> {
    public ValueClass interfaceClass;

    public ClassPropertyInterface(int ID, ValueClass iClass) {
        super(ID);
        interfaceClass = iClass;
    }
}
