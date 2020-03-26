package lsfusion.server.logics.property.value;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.cases.CaseExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.classes.data.FormulaProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class NullValueProperty extends FormulaProperty<PropertyInterface> {

    private NullValueProperty() {
        super(LocalizedString.create("Значение NULL"), SetFact.EMPTYORDER());

        finalizeInit();
    }

    public static final NullValueProperty instance = new NullValueProperty();

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return CaseExpr.NULL();
    }

    @Override
    protected Inferred<PropertyInterface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return Inferred.FALSE();
    }

    @Override
    public boolean calcNeedInferredForValueClass(InferType inferType) {
        return false;
    }

    @Override
    protected ExClassSet calcInferValueClass(ImMap<PropertyInterface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.FALSE;
    }
}
