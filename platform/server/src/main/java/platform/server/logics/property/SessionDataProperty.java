package platform.server.logics.property;

import platform.base.Pair;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.add.MAddExclMap;
import platform.server.caches.ManualLazy;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.PropertyChanges;

public class SessionDataProperty extends DataProperty {

    public SessionDataProperty(String sID, String caption, ValueClass value) {
        this(sID, caption, new ValueClass[0], value);
    }

    public SessionDataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, classes, value);

        finalizeInit();
    }

    @Override
    public Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(propClasses)
            return getClassTableExpr(joinImplement);
        if(propChanges.isEmpty())
            return CaseExpr.NULL;
        return super.calculateExpr(joinImplement, propClasses, propChanges, changedWhere);
    }

    public boolean isStored() {
        return false;
    }

    // тяжело использовать так как могут пересекаться и создавать каскадные эффекты
    // по аналогии с IsClassProperty
    public final static MAddExclMap<Pair<ImMap<ValueClass, Integer>, ValueClass>, CalcPropertyImplement<?, ValueClass>> cacheClasses = MapFact.mBigStrongMap();
    @ManualLazy
    public static <T extends PropertyInterface, P extends PropertyInterface> CalcPropertyMapImplement<?, T> getProperty(ValueClass value, ImMap<T, ValueClass> classes) {
        Pair<ImMap<ValueClass, Integer>, ValueClass> multiClasses = new Pair<ImMap<ValueClass, Integer>, ValueClass>(classes.values().multiSet(), value);
        CalcPropertyImplement<P, ValueClass> implement = (CalcPropertyImplement<P, ValueClass>) cacheClasses.get(multiClasses);
        if(implement==null) {
            CalcPropertyMapImplement<?, T> classImplement = DerivedProperty.createDataProp(true, classes, value);
            cacheClasses.exclAdd(multiClasses, classImplement.mapImplement(classes));
            return classImplement;
        } else
            return new CalcPropertyMapImplement<P, T>(implement.property, MapFact.mapValues(implement.mapping, classes));
    }

}

