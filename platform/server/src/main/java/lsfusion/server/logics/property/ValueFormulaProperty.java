package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.ConcreteValueClass;

// вообще Collection
abstract class ValueFormulaProperty<T extends PropertyInterface> extends FormulaProperty<T> {

    ConcreteValueClass value; // can be null

    protected ValueFormulaProperty(String sID, String caption, ImOrderSet<T> interfaces, ConcreteValueClass value) {
        super(sID, caption, interfaces);

        this.value = value;
    }
}
