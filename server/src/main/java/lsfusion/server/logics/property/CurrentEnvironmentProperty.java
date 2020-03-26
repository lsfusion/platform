package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.value.CurrentEnvironmentExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class CurrentEnvironmentProperty extends NoIncrementProperty<PropertyInterface> {
    
    private final String paramString; 
    private final AndClassSet paramClass;

    public CurrentEnvironmentProperty(LocalizedString caption, String paramString, AndClassSet paramClass) {
        super(caption, SetFact.EMPTYORDER());
        this.paramString = paramString;
        this.paramClass = paramClass;
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return new CurrentEnvironmentExpr(paramString, paramClass);
    }

    @Override
    protected Inferred<PropertyInterface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return Inferred.EMPTY();
    }

    @Override
    public boolean calcNeedInferredForValueClass(InferType inferType) {
        return false;
    }

    @Override
    protected ExClassSet calcInferValueClass(ImMap<PropertyInterface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.toEx(paramClass.toResolve());
    }

    @Override
    public boolean checkAlwaysNull(boolean constraint) {
        return true;
    }
}
