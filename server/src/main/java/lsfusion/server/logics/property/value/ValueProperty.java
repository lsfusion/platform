package lsfusion.server.logics.property.value;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.StaticValueExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.StaticClass;
import lsfusion.server.logics.property.NoIncrementProperty;
import lsfusion.server.logics.property.infer.CalcType;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ValueProperty extends NoIncrementProperty<PropertyInterface> {

    public final Object value;
    public final StaticClass staticClass;
    
    public static void checkLocalizedString(Object value, StaticClass staticClass) {
        StaticValueExpr.checkLocalizedString(value, staticClass);
    } 

    public ValueProperty(LocalizedString caption, Object value, StaticClass staticClass) {
        super(caption, SetFact.<PropertyInterface>EMPTYORDER());
        this.value = value;
        this.staticClass = staticClass;

        finalizeInit();

        checkLocalizedString(value, staticClass);
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return staticClass.getStaticExpr(value);
    }

    @Override
    public ExClassSet calcInferValueClass(ImMap<PropertyInterface, ExClassSet> inferred, InferType inferType) {
        return new ExClassSet(staticClass.getResolveSet(), value);
    }
}
