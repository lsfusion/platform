package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.value.Time;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.value.TimeExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.classes.infer.CalcType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class TimeFormulaProperty extends ValueFormulaProperty<PropertyInterface> {

    private final Time time;

    public TimeFormulaProperty(LocalizedString caption, Time time) {
        super(caption, SetFact.<PropertyInterface>EMPTYORDER(), time.getConcreteValueClass());

        this.time = time;

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return new TimeExpr(time);
    }
}
