package platform.server.logics.property;

import platform.server.classes.ConcreteValueClass;

import java.util.Collection;

// вообще Collection
abstract class ValueFormulaProperty<T extends PropertyInterface> extends FormulaProperty<T> {

    ConcreteValueClass value;

    protected ValueFormulaProperty(String sID, String caption, Collection<T> interfaces, ConcreteValueClass value) {
        super(sID, caption, interfaces);

        this.value = value;
    }
}
