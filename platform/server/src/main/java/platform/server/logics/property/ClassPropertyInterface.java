package platform.server.logics.property;

import platform.server.classes.ValueClass;

public class ClassPropertyInterface extends PropertyInterface<ClassPropertyInterface> {
    public ValueClass interfaceClass;

    public ClassPropertyInterface(int ID, ValueClass interfaceClass) {
        super(ID);
        this.interfaceClass = interfaceClass;
    }
}
