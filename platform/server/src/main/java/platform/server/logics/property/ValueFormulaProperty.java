package platform.server.logics.property;

import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.server.classes.ConcreteValueClass;

// вообще Collection
abstract class ValueFormulaProperty<T extends PropertyInterface> extends FormulaProperty<T> {

    ConcreteValueClass value; // can be null

    protected ValueFormulaProperty(String sID, String caption, ImOrderSet<T> interfaces, ConcreteValueClass value) {
        super(sID, caption, interfaces);

        this.value = value;
    }
}
