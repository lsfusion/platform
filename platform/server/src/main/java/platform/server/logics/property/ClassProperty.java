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
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.sql.SQLException;
import java.util.*;

public class ClassProperty extends AggregateProperty<ClassPropertyInterface> {

    public ClassProperty(String sID, String caption, ValueClass valueClass) {
        super(sID, caption, DataProperty.getInterfaces(new ValueClass[]{valueClass}));
    }

    public static Map<ClassPropertyInterface, ValueClass> getMapClasses(Collection<ClassPropertyInterface> interfaces) {
        Map<ClassPropertyInterface, ValueClass> result = new HashMap<ClassPropertyInterface, ValueClass>();
        for(ClassPropertyInterface propertyInterface : interfaces)
            result.put(propertyInterface,propertyInterface.interfaceClass);
        return result;
    }

    public static Set<ValueClass> getValueClasses(Collection<ClassPropertyInterface> interfaces) {
        Set<ValueClass> interfaceClasses = new HashSet<ValueClass>();
        for(ClassPropertyInterface valueInterface : interfaces)
            interfaceClasses.add(valueInterface.interfaceClass);
        return interfaceClasses;
    }

    public final static Map<Map<ValueClass, Integer>, PropertyImplement<?, ValueClass>> cacheClasses = new HashMap<Map<ValueClass, Integer>, PropertyImplement<?, ValueClass>>();
    @ManualLazy
    public static <T, P extends PropertyInterface> PropertyImplement<?, T> getIsClassProperty(Map<T, ValueClass> classes) {
        Map<ValueClass, Integer> multiClasses = BaseUtils.multiSet(classes.values());
        PropertyImplement<P, ValueClass> implement = (PropertyImplement<P, ValueClass>) cacheClasses.get(multiClasses);
        if(implement==null) {
            PropertyImplement<?, T> classImplement = DerivedProperty.createCProp(LogicalClass.instance, true, classes);
            cacheClasses.put(multiClasses, classImplement.mapImplement(classes));
            return classImplement;
        } else
            return new PropertyImplement<P, T>(implement.property, BaseUtils.mapValues(implement.mapping, classes));
    }

    public static <T> PropertyImplement<?, T> getIsClassProperty(ValueClass valueClass, T map) {
        ClassProperty classProperty = valueClass.getProperty();
        return new PropertyImplement<ClassPropertyInterface, T>(classProperty, Collections.singletonMap(BaseUtils.single(classProperty.interfaces), map));
    }

    public static PropertyImplement<?, ClassPropertyInterface> getIsClassProperty(Collection<ClassPropertyInterface> interfaces) {
         return getIsClassProperty(getMapClasses(interfaces));
     }

    public static <T> QuickSet<Property> getIsClassUsed(Map<T, ValueClass> joinClasses, StructChanges propChanges) {
        return getIsClassProperty(joinClasses).property.getUsedChanges(propChanges);
    }
    public static <T> Where getIsClassWhere(Map<T, ValueClass> joinClasses, Map<T, ? extends Expr> joinImplement, Modifier modifier) {
        return getIsClassWhere(joinClasses, joinImplement, modifier.getPropertyChanges(), null);
    }
    public static <T> Where getIsClassWhere(Map<T, ValueClass> joinClasses, Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getIsClassProperty(joinClasses).mapExpr(joinImplement, propChanges, changedWhere).getWhere();
    }

    public static QuickSet<Property> getIsClassUsed(ValueClass valueClass, StructChanges propChanges) {
        return getIsClassProperty(valueClass, "value").property.getUsedChanges(propChanges);
    }
    public static Where getIsClassWhere(ValueClass valueClass, Expr valueExpr, Modifier modifier) {
        return getIsClassWhere(valueClass, valueExpr, modifier.getPropertyChanges(), null);
    }
    public static Where getIsClassWhere(ValueClass valueClass, Expr valueExpr, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getIsClassProperty(valueClass, "value").mapExpr(Collections.singletonMap("value", valueExpr), propChanges, changedWhere).getWhere();
    }

    public static QuickSet<Property> getIsClassUsed(Collection<ClassPropertyInterface> interfaces, StructChanges propChanges) {
        return getIsClassProperty(interfaces).property.getUsedChanges(propChanges);
    }
    public static Where getIsClassWhere(Map<ClassPropertyInterface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getIsClassProperty(joinImplement.keySet()).mapExpr(joinImplement, propChanges, changedWhere).getWhere();
    }

    public QuickSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return QuickSet.EMPTY();
    }

    public ValueClass getValueClass() {
        return BaseUtils.single(interfaces).interfaceClass;
    }
    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ValueExpr.get(BaseUtils.singleValue(joinImplement).isClass(getValueClass().getUpSet()));
    }

    protected boolean useSimpleIncrement() {
        return true;
    }

    @Override
    protected void proceedNotNull(Map<ClassPropertyInterface, KeyExpr> mapKeys, Where where, DataSession session) throws SQLException {
        ValueClass valueClass = getValueClass();
        if(valueClass instanceof ConcreteObjectClass)
            for(Map<ClassPropertyInterface, DataObject> row : new Query<ClassPropertyInterface, Object>(mapKeys, where).executeClasses(session.sql, session.env, session.baseClass).keySet())
                session.changeClass(BaseUtils.singleValue(row), (ConcreteObjectClass) valueClass);
    }

    @Override
    protected void proceedNull(Map<ClassPropertyInterface, KeyExpr> mapKeys, Where where, DataSession session) throws SQLException {
        ValueClass valueClass = getValueClass();
        if(valueClass instanceof ConcreteObjectClass)
            for(Map<ClassPropertyInterface, DataObject> row : new Query<ClassPropertyInterface, Object>(mapKeys, where).executeClasses(session.sql, session.env, session.baseClass).keySet())
                session.changeClass(BaseUtils.singleValue(row), session.baseClass.unknown);
    }
}
