package platform.server.logics.property;

import platform.server.classes.BaseClass;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.CurrentEnvironmentExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class CurrentEnvironmentFormulaProperty extends FormulaProperty<PropertyInterface> {

    private final String paramString; 
    private final AndClassSet paramClass;

    public CurrentEnvironmentFormulaProperty(String sID, String caption, String paramString, AndClassSet paramClass) {
        super(sID, caption, new ArrayList<PropertyInterface>());
        this.paramString = paramString;
        this.paramClass = paramClass;
    }

    protected Expr calculateExpr(Map<PropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return new CurrentEnvironmentExpr(paramString, paramClass);
    }
}
