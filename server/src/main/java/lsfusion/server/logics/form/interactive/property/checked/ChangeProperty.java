package lsfusion.server.logics.form.interactive.property.checked;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class ChangeProperty<T extends PropertyInterface> extends AggregateProperty<T> {

    public ChangeProperty(LocalizedString caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        throw new RuntimeException("not supported"); // can not be stored / modified;
    }

    @Override
    protected Inferred<T> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        // they are used only in filters and in filters there is ignoreInInterface
        throw new UnsupportedOperationException();
    }

    @Override
    protected ExClassSet calcInferValueClass(ImMap<T, ExClassSet> inferred, InferType inferType) {
        // they are used only in filters and in filters there is ignoreInInterface
        throw new UnsupportedOperationException();
    }
}
