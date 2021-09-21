package lsfusion.server.logics.property.data;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.infer.CalcClassType;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class AbstractDataProperty extends Property<ClassPropertyInterface> {

    public AbstractDataProperty(LocalizedString caption, ImOrderSet<ClassPropertyInterface> interfaces) {
        super(caption, interfaces);
    }

    @Override
    public ClassWhere<Object> calcClassValueWhere(CalcClassType calcType) {
        return getDataClassValueWhere();
    }

    protected Inferred<ClassPropertyInterface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return new Inferred<>(getDataClassValueWhere().getCommonExClasses(interfaces));
    }

    @Override
    public boolean calcNeedInferredForValueClass(InferType inferType) {
        return false;
    }

    protected ExClassSet calcInferValueClass(ImMap<ClassPropertyInterface, ExClassSet> inferred, InferType inferType) {
        return getDataClassValueWhere().getCommonExClasses(SetFact.singleton("value")).get("value");
    }

    protected abstract ClassWhere<Object> getDataClassValueWhere();

    @Override
    public boolean usesSession() {
        return true;
    }

    @Override
    public boolean hasAlotKeys() {
        return false;
    }
}
