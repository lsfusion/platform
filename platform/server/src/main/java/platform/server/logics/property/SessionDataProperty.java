package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.server.caches.ManualLazy;
import platform.server.classes.LogicalClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.PropertyChanges;

import java.util.HashMap;
import java.util.Map;

public class SessionDataProperty extends DataProperty {

    public SessionDataProperty(String sID, String caption, ValueClass value) {
        this(sID, caption, new ValueClass[0], value);
    }

    public SessionDataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, classes, value);

        finalizeInit();
    }

    @Override
    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(propClasses)
            return getClassTableExpr(joinImplement);
        return CaseExpr.NULL;
    }

    public boolean isStored() {
        return false;
    }

    // тяжело использовать так как могут пересекаться и создавать каскадные эффекты
    // по аналогии с IsClassProperty
    public final static Map<Pair<Map<ValueClass, Integer>, ValueClass>, CalcPropertyImplement<?, ValueClass>> cacheClasses = new HashMap<Pair<Map<ValueClass, Integer>, ValueClass>, CalcPropertyImplement<?, ValueClass>>();
    @ManualLazy
    public static <T extends PropertyInterface, P extends PropertyInterface> CalcPropertyMapImplement<?, T> getProperty(ValueClass value, Map<T, ValueClass> classes) {
        Pair<Map<ValueClass, Integer>, ValueClass> multiClasses = new Pair<Map<ValueClass, Integer>, ValueClass>(BaseUtils.multiSet(classes.values()), value);
        CalcPropertyImplement<P, ValueClass> implement = (CalcPropertyImplement<P, ValueClass>) cacheClasses.get(multiClasses);
        if(implement==null) {
            CalcPropertyMapImplement<?, T> classImplement = DerivedProperty.createDataProp(true, classes, value);
            cacheClasses.put(multiClasses, classImplement.mapImplement(classes));
            return classImplement;
        } else
            return new CalcPropertyMapImplement<P, T>(implement.property, BaseUtils.mapValues(implement.mapping, classes));
    }

}

