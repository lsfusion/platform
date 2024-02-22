package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.property.NoIncrementProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.oraction.PropertyInterface;
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
