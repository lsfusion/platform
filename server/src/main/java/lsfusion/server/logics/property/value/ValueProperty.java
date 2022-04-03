package lsfusion.server.logics.property.value;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.value.StaticValueExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.StaticClass;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.NoIncrementProperty;
import lsfusion.server.logics.property.classes.data.FormulaProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ValueProperty extends StaticValueProperty {

    public final Object value;
    public final StaticClass staticClass;
    
    public static void checkLocalizedString(Object value, StaticClass staticClass) {
        StaticValueExpr.checkLocalizedString(value, staticClass);
    } 

    public ValueProperty(LocalizedString caption, Object value, StaticClass staticClass) {
        super(caption, SetFact.EMPTYORDER());
        this.value = value;
        this.staticClass = staticClass;

        finalizeInit();

        checkLocalizedString(value, staticClass);
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return staticClass.getStaticExpr(value);
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
    public ExClassSet calcInferValueClass(ImMap<PropertyInterface, ExClassSet> inferred, InferType inferType) {
        return new ExClassSet(staticClass.getResolveSet(), value);
    }

    @Override
    public Object getStaticValue() {
        return value;
    }
}
