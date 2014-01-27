package lsfusion.server.logics.property;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.cases.CaseExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.session.PropertyChanges;

public class SessionDataProperty extends DataProperty {

    public SessionDataProperty(String sID, String caption, ValueClass value) {
        this(sID, caption, new ValueClass[0], value);
    }

    public SessionDataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, classes, value);

        finalizeInit();
    }

    @Override
    public Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(calcType.isClass())
            return getClassTableExpr(joinImplement, calcType);
        if(propChanges.isEmpty())
            return CaseExpr.NULL;
        return super.calculateExpr(joinImplement, calcType, propChanges, changedWhere);
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

