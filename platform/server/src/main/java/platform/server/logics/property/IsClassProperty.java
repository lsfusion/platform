package platform.server.logics.property;

import platform.base.Pair;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MCol;
import platform.base.col.interfaces.mutable.add.MAddExclMap;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.ManualLazy;
import platform.server.classes.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.Modifier;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

public class IsClassProperty extends AggregateProperty<ClassPropertyInterface> {

    public IsClassProperty(String sID, String caption, ValueClass valueClass) {
        super(sID, caption, getInterfaces(new ValueClass[]{valueClass}));

        finalizeInit();
    }

    public static ImMap<ClassPropertyInterface, ValueClass> getMapClasses(ImSet<ClassPropertyInterface> interfaces) {
        return interfaces.mapValues(new GetValue<ValueClass, ClassPropertyInterface>() {
            public ValueClass getMapValue(ClassPropertyInterface value) {
                return value.interfaceClass;
            }});
    }

    // по аналогии с SessionDataProperty
    public final static MAddExclMap<ImMap<ValueClass, Integer>, CalcPropertyImplement<?, ValueClass>> cacheClasses = MapFact.mBigStrongMap();
    @ManualLazy
    public static <T, P extends PropertyInterface> CalcPropertyRevImplement<?, T> getProperty(ImMap<T, ValueClass> classes) {
        ImMap<ValueClass, Integer> multiClasses = classes.values().multiSet();
        CalcPropertyImplement<P, ValueClass> implement = (CalcPropertyImplement<P, ValueClass>) cacheClasses.get(multiClasses);
        if(implement==null) {
            CalcPropertyRevImplement<?, T> classImplement = DerivedProperty.createCProp(LogicalClass.instance, true, classes);
            cacheClasses.exclAdd(multiClasses, classImplement.mapImplement(classes));
            return classImplement;
        } else
            return new CalcPropertyRevImplement<P, T>(implement.property, MapFact.mapValues(implement.mapping, classes));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> getMapProperty(ImMap<T, ValueClass> classes) {
        return CalcPropertyRevImplement.mapPropertyImplement(getProperty(classes));
    }

    public static <T> CalcPropertyRevImplement<?, T> getProperty(ValueClass valueClass, T map) {
        IsClassProperty classProperty = valueClass.getProperty();
        return new CalcPropertyRevImplement<ClassPropertyInterface, T>(classProperty, MapFact.singletonRev(classProperty.interfaces.single(), map));
    }

    public static CalcPropertyMapImplement<?, ClassPropertyInterface> getProperty(ImSet<ClassPropertyInterface> interfaces) {
        return getMapProperty(getMapClasses(interfaces));
     }

    public static <T> Where getWhere(ImMap<T, ValueClass> joinClasses, ImMap<T, ? extends Expr> joinImplement, Modifier modifier) {
        return getProperty(joinClasses).mapExpr(joinImplement, modifier.getPropertyChanges(), null).getWhere();
    }
    public static Where getWhere(ValueClass valueClass, Expr valueExpr, Modifier modifier) {
        return getProperty(valueClass, "value").mapExpr(MapFact.singleton("value", valueExpr), modifier.getPropertyChanges(), null).getWhere();
    }

    public static ImOrderSet<ClassPropertyInterface> getInterfaces(final ValueClass[] classes) {
        return SetFact.toOrderExclSet(classes.length, new GetIndex<ClassPropertyInterface>() {
            public ClassPropertyInterface getMapValue(int i) {
                return new ClassPropertyInterface(i, classes[i]);
            }});
    }

    public static boolean fitClass(ConcreteClass concreteClass, ValueClass valueClass) {
        // unknown, custom, concatenateClassSet
        if(concreteClass instanceof ValueClass)
            return valueClass.getUpSet().containsAll(concreteClass);
        else {
            assert concreteClass instanceof UnknownClass; // с concatenate'ами надо будет разбираться
            return false;
        }
    }

    public static boolean fitInterfaceClasses(ImMap<ClassPropertyInterface, ConcreteClass> mapValues) {
        for(int i=0,size=mapValues.size();i<size;i++)
            if(!fitClass(mapValues.getValue(i), mapValues.getKey(i).interfaceClass))
                return false;
        return true;
    }

    public static boolean fitClasses(ImMap<ClassPropertyInterface, ConcreteClass> mapValues, ValueClass valueClass, ConcreteClass value) { // оптимизация
        return !(value != null && !fitClass(value, valueClass)) && fitInterfaceClasses(mapValues);
    }

    public ImSet<CalcProperty> calculateUsedChanges(StructChanges propChanges, boolean cascade) {
        return SetFact.EMPTY();
    }

    @Override
    protected ImCol<Pair<Property<?>, LinkType>> calculateLinks() {
        ImCol<Pair<Property<?>, LinkType>> actionChangeProps = getActionChangeProps(); // чтобы обnull'ить использование
        assert actionChangeProps.isEmpty();
        assert getDepends().isEmpty();

        MCol<Pair<Property<?>, LinkType>> mResult = ListFact.mCol();
        mResult.add(new Pair<Property<?>, LinkType>(getChanged(IncrementType.DROP), LinkType.DEPEND));
        mResult.add(new Pair<Property<?>, LinkType>(getChanged(IncrementType.SET), LinkType.DEPEND));

        return mResult.immutableCol();
    }

    protected boolean useSimpleIncrement() {
        return true;
    }

    public ValueClass getInterfaceClass() {
        return interfaces.single().interfaceClass;
    }
    public Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ValueExpr.get(joinImplement.singleValue().isClass(getInterfaceClass().getUpSet()));
    }

    @Override
    public ActionPropertyMapImplement<?, ClassPropertyInterface> getSetNotNullAction(boolean notNull) {
        ValueClass valueClass = getInterfaceClass();
        if(valueClass instanceof ConcreteCustomClass) {
            ActionProperty<PropertyInterface> changeClassAction = (notNull ? (ConcreteCustomClass) valueClass : ((ConcreteCustomClass) valueClass).getBaseClass().unknown).getChangeClassAction();
            return new ActionPropertyMapImplement<PropertyInterface, ClassPropertyInterface>(changeClassAction, MapFact.singletonRev(changeClassAction.interfaces.single(), interfaces.single()));
        }
        return null;
    }

    public Where getRemoveWhere(Expr joinExpr, PropertyChanges newChanges) {
        return getChanged(IncrementType.DROP).getExpr(MapFact.singleton(interfaces.single(), joinExpr), newChanges).getWhere();
    }
}
