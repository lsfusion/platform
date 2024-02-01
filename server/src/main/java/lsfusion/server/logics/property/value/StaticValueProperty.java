package lsfusion.server.logics.property.value;

import lsfusion.base.col.SetFact;
import lsfusion.server.logics.property.classes.data.FormulaProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class StaticValueProperty extends FormulaProperty<PropertyInterface> {

    public StaticValueProperty(LocalizedString caption) {
        super(caption, SetFact.EMPTYORDER());
    }

    public abstract Object getStaticValue();

    public static <T extends PropertyInterface> Inferred<T> inferInterfaceClasses(InferType inferType, ExClassSet commonValue) {
        return Inferred.EMPTY();
    }
}
