package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.DataClass;
import lsfusion.server.logics.i18n.LocalizedString;

// вообще Collection
abstract class ValueFormulaProperty<T extends PropertyInterface> extends FormulaProperty<T> {

    DataClass value; // can be null

    protected ValueFormulaProperty(LocalizedString caption, ImOrderSet<T> interfaces, DataClass value) {
        super(caption, interfaces);

        this.value = value;
    }
}
