package platform.server.logics.properties;

import platform.server.data.PropertyField;
import platform.server.data.Source;
import platform.server.data.query.GroupQuery;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.UnionQuery;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.logics.classes.sets.*;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataSession;

import java.util.*;

abstract public class GroupProperty<T extends PropertyInterface> extends MapProperty<GroupPropertyInterface<T>,T,T,GroupPropertyInterface<T>,Object> {
    // каждый интерфейс должен имплементировать именно GetInterface GroupProperty

    // оператор
    int operator;

    protected GroupProperty(String iSID, Collection<GroupPropertyInterface<T>> iInterfaces, TableFactory iTableFactory, Property<T> iProperty,int iOperator) {
        super(iSID, iInterfaces, iTableFactory);
        groupProperty = iProperty;
        operator = iOperator;
    }

    // группировочное св-во собсно должно быть не формулой
    Property<T> groupProperty;

    InterfaceClassSet<T> getImplementSet(InterfaceClass<GroupPropertyInterface<T>> ClassImplement) {
        InterfaceClassSet<T> ValueClassSet = groupProperty.getUniversalInterface();
        for(Map.Entry<GroupPropertyInterface<T>, ClassSet> Class : ClassImplement.entrySet())
            ValueClassSet = ValueClassSet.and(Class.getKey().implement.mapClassSet(Class.getValue()));
        return ValueClassSet;
    }

    InterfaceClassSet<T> getMapClassSet(MapRead<T> Read, InterfaceClassSet<T> GroupClassSet, InterfaceClassSet<GroupPropertyInterface<T>> Result) {
        // сначала делаем and всех classSet'ов, а затем getValueClass
        for(GroupPropertyInterface<T> Interface : interfaces)
            GroupClassSet = GroupClassSet.and(Read.getImplementClassSet(Interface.implement,ClassSet.universal));
        for(InterfaceClass<T> GroupImplement : GroupClassSet) {
            InterfaceClass<GroupPropertyInterface<T>> ValueClass = new InterfaceClass<GroupPropertyInterface<T>>();
            for(GroupPropertyInterface<T> Interface : interfaces)
                ValueClass.put(Interface,Read.getImplementValueClass(Interface.implement,GroupImplement));
            Result.or(new InterfaceClassSet<GroupPropertyInterface<T>>(ValueClass));
        }

        return GroupClassSet;
    }

    List<GroupPropertyInterface> GetChangedProperties(DataSession Session) {
        List<GroupPropertyInterface> ChangedProperties = new ArrayList<GroupPropertyInterface>();
        // должен вернуть null если нету изменений (или просто транслирует интерфейс) иначе возвращает AggregateProperty
        for(GroupPropertyInterface Interface : interfaces)
            if(Interface.implement.mapHasChanges(Session)) ChangedProperties.add(Interface);

        return ChangedProperties;
    }

    Property<T> getMapProperty() {
        return groupProperty;
    }

    Map<GroupPropertyInterface<T>, PropertyInterfaceImplement<T>> getMapImplements() {
        Map<GroupPropertyInterface<T>,PropertyInterfaceImplement<T>> Result = new HashMap<GroupPropertyInterface<T>,PropertyInterfaceImplement<T>>();
        for(GroupPropertyInterface<T> Interface : interfaces)
            Result.put(Interface,Interface.implement);
        return Result;
    }

    Collection<T> getMapInterfaces() {
        return groupProperty.interfaces;
    }

    void fillChangedRead(UnionQuery<T, Object> listQuery, Object value, MapChangedRead<T> read, ValueClassSet<GroupPropertyInterface<T>> readClasses) {
        // создается JoinQuery - на вход getMapInterfaces, Query.MapKeys - map интерфейсов
        InterfaceClassSet<T> mapClasses = new InterfaceClassSet<T>();
        for(ChangeClass<T> change : read.getMapChangeClass(groupProperty)) {
            InterfaceClassSet<GroupPropertyInterface<T>> resultInterface = new InterfaceClassSet<GroupPropertyInterface<T>>();
            InterfaceClassSet<T> groupInterface = getMapClassSet(read, change.interfaceClasses, resultInterface);
            if(!groupInterface.isEmpty()) {
                readClasses.or(new ChangeClass<GroupPropertyInterface<T>>(resultInterface,change.value));
                mapClasses.or(groupInterface);
            }
        }
        if(mapClasses.isEmpty()) return;

        JoinQuery<T,Object> query = new JoinQuery<T,Object>(groupProperty.interfaces);
        Map<PropertyInterfaceImplement<T>, SourceExpr> implementExprs = new HashMap<PropertyInterfaceImplement<T>, SourceExpr>();
        for(GroupPropertyInterface<T> propertyInterface : interfaces) {
            SourceExpr implementExpr = read.getImplementExpr(propertyInterface.implement, query.mapKeys, mapClasses);
            query.properties.put(propertyInterface, implementExpr);
            implementExprs.put(propertyInterface.implement,implementExpr);
        }
        read.fillMapExpr(query, value, groupProperty, query.mapKeys,implementExprs,mapClasses);
        listQuery.add(query);
    }

    public ClassSet calculateValueClass(InterfaceClass<GroupPropertyInterface<T>> ClassImplement) {

        ClassSet Result = new ClassSet();
        for(InterfaceClass<T> GroupImplement : getImplementSet(ClassImplement))
            Result.or(groupProperty.getValueClass(GroupImplement));
        return Result;
    }

    public InterfaceClassSet<GroupPropertyInterface<T>> calculateClassSet(ClassSet reqValue) {
        InterfaceClassSet<GroupPropertyInterface<T>> Result = new InterfaceClassSet<GroupPropertyInterface<T>>();
        getMapClassSet(DBRead, groupProperty.getClassSet(reqValue),Result);
        return Result;
    }

    public ValueClassSet<GroupPropertyInterface<T>> calculateValueClassSet() {
        ValueClassSet<GroupPropertyInterface<T>> Result = new ValueClassSet<GroupPropertyInterface<T>>();
        for(ChangeClass<T> ImplementChange : groupProperty.getValueClassSet()) {
            InterfaceClassSet<GroupPropertyInterface<T>> ImplementInterface = new InterfaceClassSet<GroupPropertyInterface<T>>();
            getMapClassSet(DBRead,ImplementChange.interfaceClasses,ImplementInterface);
            Result.or(new ChangeClass<GroupPropertyInterface<T>>(ImplementInterface,ImplementChange.value));
        }
        return Result;
    }

    Object GroupValue = "grfield";

    SourceExpr calculateSourceExpr(Map<GroupPropertyInterface<T>,? extends SourceExpr> joinImplement, InterfaceClassSet<GroupPropertyInterface<T>> joinClasses) {

        InterfaceClassSet<T> ImplementClasses = new InterfaceClassSet<T>();
        for(InterfaceClass<GroupPropertyInterface<T>> JoinClass : joinClasses)
            ImplementClasses.or(getImplementSet(JoinClass));

        JoinQuery<T,Object> Query = new JoinQuery<T,Object>(groupProperty.interfaces);
        for(GroupPropertyInterface<T> Interface : interfaces)
            Query.properties.put(Interface, Interface.implement.mapSourceExpr(Query.mapKeys,ImplementClasses));
        Query.properties.put(GroupValue, groupProperty.getSourceExpr(Query.mapKeys, ImplementClasses));

        return (new Join<GroupPropertyInterface<T>,Object>(new GroupQuery<Object,GroupPropertyInterface<T>,Object,T>(interfaces,Query,GroupValue, operator), joinImplement)).exprs.get(GroupValue);
    }

    Map<Object, Type> getMapNullProps(Object Value) {
        Map<Object, Type> NullProps = new HashMap<Object,Type>();
        NullProps.put(Value, getType());
        InterfaceClass<GroupPropertyInterface<T>> InterfaceClass = getClassSet(ClassSet.universal).iterator().next();
        for(Map.Entry<GroupPropertyInterface<T>,ClassSet> Interface : InterfaceClass.entrySet())
            NullProps.put(Interface.getKey(),Interface.getValue().getType());
        return NullProps;
    }

    Source<GroupPropertyInterface<T>, PropertyField> getGroupQuery(List<MapChangedRead<T>> ReadList, PropertyField Value, ValueClassSet<GroupPropertyInterface<T>> ResultClass) {
        return new GroupQuery<Object,GroupPropertyInterface<T>, PropertyField,T>(interfaces,getMapQuery(ReadList,Value,ResultClass,false),Value, operator);
    }
}
