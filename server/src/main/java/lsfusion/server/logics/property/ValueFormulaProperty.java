package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.classes.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// вообще Collection
public abstract class ValueFormulaProperty<T extends PropertyInterface> extends FormulaProperty<T> {

    protected DataClass value; // can be null

    protected ValueFormulaProperty(LocalizedString caption, ImOrderSet<T> interfaces, DataClass value) {
        super(caption, interfaces);

        this.value = value;
    }
}
