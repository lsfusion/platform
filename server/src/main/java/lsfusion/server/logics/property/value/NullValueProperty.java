package lsfusion.server.logics.property.value;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.cases.CaseExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.property.infer.CalcType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.classes.data.FormulaProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;
import lsfusion.server.logics.property.infer.Inferred;
import lsfusion.server.logics.action.session.change.PropertyChanges;

public class NullValueProperty extends FormulaProperty<PropertyInterface> {

    private NullValueProperty() {
        super(LocalizedString.create("Значение NULL"), SetFact.<PropertyInterface>EMPTYORDER());

        finalizeInit();
    }

    public static final NullValueProperty instance = new NullValueProperty();

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return CaseExpr.NULL;
    }

    @Override
    protected Inferred<PropertyInterface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return Inferred.FALSE();
    }
}
