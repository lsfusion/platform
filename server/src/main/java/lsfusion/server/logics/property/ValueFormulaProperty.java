package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.ConcreteValueClass;
import lsfusion.server.classes.DataClass;

// вообще Collection
abstract class ValueFormulaProperty<T extends PropertyInterface> extends FormulaProperty<T> {

    DataClass value; // can be null

    protected ValueFormulaProperty(String caption, ImOrderSet<T> interfaces, DataClass value) {
        super(caption, interfaces);

        this.value = value;
    }
}
