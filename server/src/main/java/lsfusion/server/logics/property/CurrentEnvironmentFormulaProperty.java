package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.CurrentEnvironmentExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.session.PropertyChanges;

public abstract class CurrentEnvironmentFormulaProperty extends FormulaProperty<PropertyInterface> {

    private final String paramString; 
    private final AndClassSet paramClass;

    public CurrentEnvironmentFormulaProperty(String sID, String caption, String paramString, AndClassSet paramClass) {
        super(sID, caption, SetFact.<PropertyInterface>EMPTYORDER());
        this.paramString = paramString;
        this.paramClass = paramClass;
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return new CurrentEnvironmentExpr(paramString, paramClass);
    }
}
