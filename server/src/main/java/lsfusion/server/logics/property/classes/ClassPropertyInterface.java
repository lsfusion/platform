package lsfusion.server.logics.property.classes;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ClassPropertyInterface extends PropertyInterface<ClassPropertyInterface> {
    public final ValueClass interfaceClass;

    public ClassPropertyInterface(int ID, ValueClass interfaceClass) {
        super(ID);
        this.interfaceClass = interfaceClass;
//        assert interfaceClass != null; // ignoreFitClassesCheck
    }
}
