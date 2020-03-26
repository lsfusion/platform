package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// вообще Collection
public abstract class ValueFormulaProperty<T extends PropertyInterface> extends FormulaProperty<T> {

    protected DataClass value; // can be null for String

    protected ValueFormulaProperty(LocalizedString caption, ImOrderSet<T> interfaces, DataClass value) {
        super(caption, interfaces);

        this.value = value;
    }

    @Override
    public boolean calcNeedInferredForValueClass(InferType inferType) {
        return false;
    }

    @Override
    protected ExClassSet calcInferValueClass(ImMap<T, ExClassSet> inferred, InferType inferType) {
        assert value != null; // for String this method is overriden
        return ExClassSet.toExValue(value);
    }
}
