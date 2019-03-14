package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.property.NoIncrementProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

abstract public class FormulaProperty<T extends PropertyInterface> extends NoIncrementProperty<T> {

    protected FormulaProperty(LocalizedString caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    @Override
    public boolean checkAlwaysNull(boolean constraint) {
        return true;
    }
}
