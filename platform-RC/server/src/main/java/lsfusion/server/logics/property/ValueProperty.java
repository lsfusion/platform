package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.StaticClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;
import lsfusion.server.session.PropertyChanges;

public class ValueProperty extends NoIncrementProperty<PropertyInterface> {

    public final Object value;
    public final StaticClass staticClass;
    
    public static void checkLocalizedString(Object value, StaticClass staticClass) {
        assert !(staticClass instanceof StringClass) || value instanceof LocalizedString;
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
