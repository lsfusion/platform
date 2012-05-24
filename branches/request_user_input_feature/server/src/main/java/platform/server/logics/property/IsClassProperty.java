package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.caches.ManualLazy;
import platform.server.classes.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

public class IsClassProperty extends AggregateProperty<ClassPropertyInterface> {

    public IsClassProperty(String sID, String caption, ValueClass valueClass) {
        super(sID, caption, getInterfaces(new ValueClass[]{valueClass}));

        finalizeInit();
    }

    public static Map<ClassPropertyInterface, ValueClass> getMapClasses(Collection<ClassPropertyInterface> interfaces) {
        Map<ClassPropertyInterface, ValueClass> result = new HashMap<ClassPropertyInterface, ValueClass>();
        for(ClassPropertyInterface propertyInterface : interfaces)
            result.put(propertyInterface,propertyInterface.interfaceClass);
        return result;
    }

    public final static Map<Map<ValueClass, Integer>, CalcPropertyImplement<?, ValueClass>> cacheClasses = new HashMap<Map<ValueClass, Integer>, CalcPropertyImplement<?, ValueClass>>();
    @ManualLazy
    public static <T, P extends PropertyInterface> CalcPropertyImplement<?, T> getProperty(Map<T, ValueClass> classes) {
        Map<ValueClass, Integer> multiClasses = BaseUtils.multiSet(classes.values());
        CalcPropertyImplement<P, ValueClass> implement = (CalcPropertyImplement<P, ValueClass>) cacheClasses.get(multiClasses);
        if(implement==null) {
            CalcPropertyImplement<?, T> classImplement = DerivedProperty.createCProp(LogicalClass.instance, true, classes);
            cacheClasses.put(multiClasses, classImplement.mapImplement(classes));
            return classImplement;
        } else
            return new CalcPropertyImplement<P, T>(implement.property, BaseUtils.mapValues(implement.mapping, classes));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> getMapProperty(Map<T, ValueClass> classes) {
        return CalcPropertyMapImplement.mapPropertyImplement(getProperty(classes));
    }

    public static <T> CalcPropertyImplement<?, T> getProperty(ValueClass valueClass, T map) {
        IsClassProperty classProperty = valueClass.getProperty();
        return new CalcPropertyImplement<ClassPropertyInterface, T>(classProperty, Collections.singletonMap(BaseUtils.single(classProperty.interfaces), map));
    }

    public static CalcPropertyMapImplement<?, ClassPropertyInterface> getProperty(Collection<ClassPropertyInterface> interfaces) {
        return getMapProperty(getMapClasses(interfaces));
     }

    public static <T> Where getWhere(Map<T, ValueClass> joinClasses, Map<T, ? extends Expr> joinImplement, Modifier modifier) {
        return getProperty(joinClasses).mapExpr(joinImplement, modifier.getPropertyChanges(), null).getWhere();
    }
    public static Where getWhere(ValueClass valueClass, Expr valueExpr, Modifier modifier) {
        return getProperty(valueClass, "value").mapExpr(Collections.singletonMap("value", valueExpr), modifier.getPropertyChanges(), null).getWhere();
    }

    public static List<ClassPropertyInterface> getInterfaces(ValueClass[] classes) {
        List<ClassPropertyInterface> interfaces = new ArrayList<ClassPropertyInterface>();
        for(ValueClass interfaceClass : classes)
            interfaces.add(new ClassPropertyInterface(interfaces.size(),interfaceClass));
        return interfaces;
    }

    public static boolean fitClass(ConcreteClass concreteClass, ValueClass valueClass) {
        // unknown, custom, concatenateClassSet
        if(concreteClass instanceof ValueClass)
            return valueClass.isCompatibleParent((ValueClass) concreteClass);
        else {
            assert concreteClass instanceof UnknownClass; // с concatenate'ами надо будет разбираться
            return false;
        }
    }

    public static boolean fitInterfaceClasses(Map<ClassPropertyInterface, ConcreteClass> mapValues) {
        for(Map.Entry<ClassPropertyInterface, ConcreteClass> interfaceValue : mapValues.entrySet())
            if(!fitClass(interfaceValue.getValue(), interfaceValue.getKey().interfaceClass))
                return false;
        return true;
    }

    public static boolean fitClasses(Map<ClassPropertyInterface, ConcreteClass> mapValues, ValueClass valueClass, ConcreteClass value) { // оптимизация
        return !(value != null && !fitClass(value, valueClass)) && fitInterfaceClasses(mapValues);
    }

    public QuickSet<CalcProperty> calculateUsedChanges(StructChanges propChanges, boolean cascade) {
        return QuickSet.EMPTY();
    }

    protected boolean useSimpleIncrement() {
        return true;
    }

    public ValueClass getInterfaceClass() {
        return BaseUtils.single(interfaces).interfaceClass;
    }
    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ValueExpr.get(BaseUtils.singleValue(joinImplement).isClass(getInterfaceClass().getUpSet()));
    }

    @Override
    protected void proceedNotNull(PropertySet<ClassPropertyInterface> set, ExecutionEnvironment env, boolean notNull) throws SQLException {
        ValueClass valueClass = getInterfaceClass();
        if(valueClass instanceof ConcreteObjectClass) {
            for (Iterator<Map<ClassPropertyInterface, DataObject>> iterator = set.executeClasses(env).iterator(); iterator.hasNext(); )
                env.changeClass(null, BaseUtils.singleValue(iterator.next()), notNull ? (ConcreteObjectClass) valueClass : env.getSession().baseClass.unknown, !iterator.hasNext());
        }
    }

    public static Set<CalcProperty> getParentProps(CustomClass customClass) {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        Collection<CustomClass> parents = new HashSet<CustomClass>();
        customClass.fillParents(parents);
        for(CustomClass parent : parents)
            result.add(parent.getProperty());
        return result;
    }

    @Override
    public Set<CalcProperty> getSetChangeProps(boolean notNull, boolean add) {
        // предыдущий класс может быть любым кроме child's
        CustomClass customClass = ((CustomClass) getInterfaceClass());
        Set<CalcProperty> childProps = customClass.getChildProps();
        if(add) {
            assert notNull;
            return getParentProps(customClass);
        }

        if(notNull)
            return BaseUtils.removeSet(customClass.getBaseClass().getChildProps(), childProps);
        else
            return BaseUtils.mergeSet(getParentProps(customClass), childProps);
    }

    public Where getRemoveWhere(Expr joinExpr, PropertyChanges newChanges) {
        WhereBuilder changedWhere = new WhereBuilder();
        getIncrementExpr(Collections.singletonMap(BaseUtils.single(interfaces), joinExpr), changedWhere, false, newChanges, IncrementType.DROP);
        return changedWhere.toWhere();
    }
}
