package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.classes.DoubleClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.RandomExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.action.session.change.PropertyChanges;

public class RandomFormulaProperty extends ValueFormulaProperty<PropertyInterface> {

    public RandomFormulaProperty(LocalizedString caption) {
        super(caption, SetFact.<PropertyInterface>EMPTYORDER(), DoubleClass.instance);

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return RandomExpr.instance;
    }

}
