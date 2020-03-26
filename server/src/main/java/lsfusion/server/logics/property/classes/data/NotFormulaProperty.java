package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class NotFormulaProperty extends FormulaProperty<PropertyInterface> {

    public NotFormulaProperty() {
        super(LocalizedString.create("Не"), SetFact.singletonOrder(new PropertyInterface(0)));
    }

    public final static NotFormulaProperty instance = new NotFormulaProperty();

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ValueExpr.get(joinImplement.singleValue().getWhere().not());
    }
    
    public <T> PropertyImplement<PropertyInterface , T> getImplement(T map) {
        return new PropertyImplement<>(this, MapFact.singleton(interfaces.single(), map));
    }

    @Override
    public Inferred<PropertyInterface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return Inferred.EMPTY(); // need empty and not map i -> null, because last one means that i is not null (and it is not)
    }

    @Override
    public boolean calcNeedInferredForValueClass(InferType inferType) {
        return false;
    }

    @Override
    protected ExClassSet calcInferValueClass(ImMap<PropertyInterface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.logical;
    }

}
