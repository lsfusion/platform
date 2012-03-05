package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.caches.ManualLazy;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.LogicalClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.Query;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

public class IsClassProperty extends AggregateProperty<ClassPropertyInterface> {

    public IsClassProperty(String sID, String caption, ValueClass valueClass) {
        super(sID, caption, DataProperty.getInterfaces(new ValueClass[]{valueClass}));

        finalizeInit();
    }

    public static Map<ClassPropertyInterface, ValueClass> getMapClasses(Collection<ClassPropertyInterface> interfaces) {
        Map<ClassPropertyInterface, ValueClass> result = new HashMap<ClassPropertyInterface, ValueClass>();
        for(ClassPropertyInterface propertyInterface : interfaces)
            result.put(propertyInterface,propertyInterface.interfaceClass);
        return result;
    }

    public final static Map<Map<ValueClass, Integer>, PropertyImplement<?, ValueClass>> cacheClasses = new HashMap<Map<ValueClass, Integer>, PropertyImplement<?, ValueClass>>();
    @ManualLazy
    public static <T, P extends PropertyInterface> PropertyImplement<?, T> getProperty(Map<T, ValueClass> classes) {
        Map<ValueClass, Integer> multiClasses = BaseUtils.multiSet(classes.values());
        PropertyImplement<P, ValueClass> implement = (PropertyImplement<P, ValueClass>) cacheClasses.get(multiClasses);
        if(implement==null) {
            PropertyImplement<?, T> classImplement = DerivedProperty.createCProp(LogicalClass.instance, true, classes);
            cacheClasses.put(multiClasses, classImplement.mapImplement(classes));
            return classImplement;
        } else
            return new PropertyImplement<P, T>(implement.property, BaseUtils.mapValues(implement.mapping, classes));
    }

    public static <T> PropertyImplement<?, T> getProperty(ValueClass valueClass, T map) {
        IsClassProperty classProperty = valueClass.getProperty();
        return new PropertyImplement<ClassPropertyInterface, T>(classProperty, Collections.singletonMap(BaseUtils.single(classProperty.interfaces), map));
    }

    public static PropertyImplement<?, ClassPropertyInterface> getProperty(Collection<ClassPropertyInterface> interfaces) {
         return getProperty(getMapClasses(interfaces));
     }

    public static <T> Where getWhere(Map<T, ValueClass> joinClasses, Map<T, ? extends Expr> joinImplement, Modifier modifier) {
        return getProperty(joinClasses).mapExpr(joinImplement, modifier.getPropertyChanges(), null).getWhere();
    }
    public static Where getWhere(ValueClass valueClass, Expr valueExpr, Modifier modifier) {
        return getProperty(valueClass, "value").mapExpr(Collections.singletonMap("value", valueExpr), modifier.getPropertyChanges(), null).getWhere();
    }

    public QuickSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return QuickSet.EMPTY();
    }

    protected boolean useSimpleIncrement() {
        return true;
    }

    public ValueClass getInterfaceClass() {
        return BaseUtils.single(interfaces).interfaceClass;
    }
    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ValueExpr.get(BaseUtils.singleValue(joinImplement).isClass(getInterfaceClass().getUpSet()));
    }

    @Override
    protected void proceedNotNull(Map<ClassPropertyInterface, KeyExpr> mapKeys, Where where, DataSession session, Modifier modifier) throws SQLException {
        ValueClass valueClass = getInterfaceClass();
        if(valueClass instanceof ConcreteObjectClass)
            for(Map<ClassPropertyInterface, DataObject> row : new Query<ClassPropertyInterface, Object>(mapKeys, where).executeClasses(session.sql, session.env, session.baseClass).keySet())
                session.changeClass(BaseUtils.singleValue(row), (ConcreteObjectClass) valueClass);
    }

    @Override
    protected void proceedNull(Map<ClassPropertyInterface, KeyExpr> mapKeys, Where where, DataSession session, Modifier modifier) throws SQLException {
        ValueClass valueClass = getInterfaceClass();
        if(valueClass instanceof ConcreteObjectClass)
            for(Map<ClassPropertyInterface, DataObject> row : new Query<ClassPropertyInterface, Object>(mapKeys, where).executeClasses(session.sql, session.env, session.baseClass).keySet())
                session.changeClass(BaseUtils.singleValue(row), session.baseClass.unknown);
    }

    public Where getRemoveWhere(Expr joinExpr, PropertyChanges newChanges, PropertyChanges prevChanges) {
        WhereBuilder changedWhere = new WhereBuilder();
        getIncrementExpr(Collections.singletonMap(BaseUtils.single(interfaces), joinExpr), newChanges, prevChanges, changedWhere, IncrementType.DROP);
        return changedWhere.toWhere();
    }
}
