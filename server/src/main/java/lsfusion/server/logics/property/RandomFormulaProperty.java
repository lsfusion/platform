package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.DoubleClass;
import lsfusion.server.data.Time;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.RandomExpr;
import lsfusion.server.data.expr.TimeExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.session.PropertyChanges;

public class RandomFormulaProperty extends ValueFormulaProperty<PropertyInterface> {

    public RandomFormulaProperty(String caption) {
        super(caption, SetFact.<PropertyInterface>EMPTYORDER(), DoubleClass.instance);

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return RandomExpr.instance;
    }

}
