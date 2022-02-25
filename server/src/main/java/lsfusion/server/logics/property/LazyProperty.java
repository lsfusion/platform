package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// the issue of this property is that if it is materialized, property should also be considered materialized
// but for now all lazy implementations are not very well incremented so we'll ignore that for now
public abstract class LazyProperty extends SimpleIncrementProperty<ClassPropertyInterface> {

    private PropertyMapImplement<?, ClassPropertyInterface> property;

    public LazyProperty(LocalizedString caption, ValueClass[] valueClasses) {
        super(caption, IsClassProperty.getInterfaces(valueClasses));
    }

    @Override
    protected Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return property.mapExpr(joinImplement, calcType, propChanges, changedWhere);
    }

    @Override
    protected Inferred<ClassPropertyInterface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return new Inferred<>(ExClassSet.toExValue(IsClassProperty.getMapClasses(interfaces)));
    }

    @Override
    public void finalizeInit() {
        super.finalizeInit();

        property = createProperty();
    }

    protected abstract PropertyMapImplement<?, ClassPropertyInterface> createProperty();
}
