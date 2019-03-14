package lsfusion.server.logics.property.env;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.classes.sets.AndClassSet;
import lsfusion.server.data.expr.CurrentEnvironmentExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.classes.data.FormulaProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.action.session.change.PropertyChanges;

public abstract class CurrentEnvironmentFormulaProperty extends FormulaProperty<PropertyInterface> {

    private final String paramString; 
    private final AndClassSet paramClass;

    public CurrentEnvironmentFormulaProperty(LocalizedString caption, String paramString, AndClassSet paramClass) {
        super(caption, SetFact.<PropertyInterface>EMPTYORDER());
        this.paramString = paramString;
        this.paramClass = paramClass;
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return new CurrentEnvironmentExpr(paramString, paramClass);
    }
}
