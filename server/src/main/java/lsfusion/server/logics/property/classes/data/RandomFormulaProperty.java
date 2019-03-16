package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.value.Time;
import lsfusion.server.data.expr.value.RandomExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.data.integral.DoubleClass;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class RandomFormulaProperty extends ValueFormulaProperty<PropertyInterface> {

    public RandomFormulaProperty(LocalizedString caption) {
        super(caption, SetFact.<PropertyInterface>EMPTYORDER(), DoubleClass.instance);

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return RandomExpr.instance;
    }

}
