package platform.server.logics.properties;

import platform.server.data.classes.ConcreteValueClass;

import java.util.Collection;

// вообще Collection
abstract class ValueFormulaProperty<T extends FormulaPropertyInterface> extends FormulaProperty<T> {

    ConcreteValueClass value;

    protected ValueFormulaProperty(String iSID, Collection<T> iInterfaces, ConcreteValueClass iValue) {
        super(iSID, iInterfaces);

        value = iValue;
    }
}
