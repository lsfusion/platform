package lsfusion.server.logics.property;

import lsfusion.server.classes.ValueClass;

public class ClassPropertyInterface extends PropertyInterface<ClassPropertyInterface> {
    public final ValueClass interfaceClass;

    public ClassPropertyInterface(int ID, ValueClass interfaceClass) {
        super(ID);
        this.interfaceClass = interfaceClass;
        assert interfaceClass != null;
    }
}
