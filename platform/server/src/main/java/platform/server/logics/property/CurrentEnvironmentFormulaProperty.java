package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.CurrentEnvironmentExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public abstract class CurrentEnvironmentFormulaProperty extends FormulaProperty<PropertyInterface> {

    private final String paramString; 
    private final AndClassSet paramClass;

    public CurrentEnvironmentFormulaProperty(String sID, String caption, String paramString, AndClassSet paramClass) {
        super(sID, caption, SetFact.<PropertyInterface>EMPTYORDER());
        this.paramString = paramString;
        this.paramClass = paramClass;
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return new CurrentEnvironmentExpr(paramString, paramClass);
    }
}
