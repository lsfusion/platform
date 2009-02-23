package platform.server.logics.properties;

import platform.server.logics.data.TableFactory;
import platform.server.logics.ObjectValue;
import platform.server.logics.auth.ChangePropertySecurityPolicy;
import platform.server.logics.session.DataSession;
import platform.server.logics.session.ChangeValue;
import platform.server.logics.classes.BitClass;
import platform.server.logics.classes.DataClass;
import platform.server.logics.classes.sets.*;
import platform.server.data.query.ChangeQuery;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.UnionQuery;
import platform.server.data.query.Join;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.data.PropertyField;

import java.util.*;

public class JoinProperty<T extends PropertyInterface> extends MapProperty<JoinPropertyInterface,T, JoinPropertyInterface,T, PropertyField> {
    public PropertyImplement<PropertyInterfaceImplement<JoinPropertyInterface>,T> implementations;

    public JoinProperty(TableFactory iTableFactory, Property<T> iProperty) {
        super(iTableFactory);
        implementations = new PropertyImplement<PropertyInterfaceImplement<JoinPropertyInterface>,T>(iProperty);
    }

    InterfaceClassSet<JoinPropertyInterface> getMapClassSet(MapRead<JoinPropertyInterface> Read, InterfaceClass<T> InterfaceImplement) {
        InterfaceClassSet<JoinPropertyInterface> Result = getUniversalInterface();
        for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> MapInterface : implementations.mapping.entrySet())
           Result = Result.and(Read.getImplementClassSet(MapInterface.getValue(),InterfaceImplement.get(MapInterface.getKey())));
        return Result;
    }

    public void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {

        // если только основное - Property ->I - как было (если изменилось только 2 то его и вкинем), возвр. I
        // иначе (не (основное MultiplyProperty и 1)) - Property, Implements ->0 - как было, возвр. 0 - (на подчищение - если (1 или 2) то Left Join'им старые значения)
        // иначе (основное MultiplyProperty и 1) - Implements ->1 - как было (но с другим оператором), возвр. 1

        if(!containsImplement(RequiredTypes.keySet())) {
            implementations.property.setChangeType(RequiredTypes,IncrementType);
        } else {
            int ReqType = (implementAllInterfaces() && IncrementType.equals(0)?0:2);

            implementations.property.setChangeType(RequiredTypes,ReqType);
            for(PropertyInterfaceImplement Interface : implementations.mapping.values())
                if(Interface instanceof PropertyMapImplement)
                    (((PropertyMapImplement)Interface).property).setChangeType(RequiredTypes,(implementations.property instanceof MultiplyFormulaProperty && IncrementType.equals(1)?1:ReqType));
        }
    }

    // инкрементные св-ва
    public Change incrementChanges(DataSession session, int changeType) {

        // алгоритм такой - для всех map св-в (в которых были изменения) строим подмножества изм. св-в
        // далее реализации этих св-в "замещаем" (то есть при JOIN будем подставлять), с остальными св-вами делаем LEFT JOIN на IS NULL
        // и JOIN'им с основным св-вом делая туда FULL JOIN новых значений
        // или UNION или большой FULL JOIN в нужном порядке (и не делать LEFT JOIN на IS NULL) и там сделать большой NVL

        ChangeQuery<JoinPropertyInterface,PropertyField> resultQuery = new ChangeQuery<JoinPropertyInterface, PropertyField>(interfaces); // по умолчанию на KEYNULL (но если Multiply то 1 на сумму)
        ValueClassSet<JoinPropertyInterface> resultClass = new ValueClassSet<JoinPropertyInterface>();

        int QueryIncrementType = changeType;
        if(implementations.property instanceof MultiplyFormulaProperty && changeType ==1)
            resultQuery.add(getMapQuery(getChangeImplements(session,1), changeTable.value,resultClass,true));
        else {
            // если нужна 1 и изменились св-ва то придется просто 2-ку считать (хотя это потом можно поменять)
            if(QueryIncrementType==1 && containsImplement(session.propertyChanges.keySet()))
                QueryIncrementType = 2;

            // все на Value - PrevValue не интересует, его как раз верхний подгоняет
            resultQuery.add(getMapQuery(getChange(session,QueryIncrementType==1?1:0), changeTable.value,resultClass,false));
            if(QueryIncrementType==2) resultQuery.add(getMapQuery(getPreviousChange(session), changeTable.prevValue,new ValueClassSet<JoinPropertyInterface>(),false));
        }

        return new Change(QueryIncrementType,resultQuery,resultClass);
    }

    public Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        if(!containsImplement(ChangedProps)) {
            ToWait.add(implementations.property);
            return null;
        } else
        if(implementations.property instanceof MultiplyFormulaProperty)
            return 1;
        else
            return 0;
    }

    Property<T> getMapProperty() {
        return implementations.property;
    }

    Map<T, PropertyInterfaceImplement<JoinPropertyInterface>> getMapImplements() {
        return implementations.mapping;
    }

    Collection<JoinPropertyInterface> getMapInterfaces() {
        return interfaces;
    }

    // по сути для формулы выделяем
    ValueClassSet<JoinPropertyInterface> getReadValueClassSet(MapRead<JoinPropertyInterface> Read, InterfaceClassSet<T> MapClasses) {
        ValueClassSet<JoinPropertyInterface> Result = new ValueClassSet<JoinPropertyInterface>();

        if(implementations.property instanceof ObjectFormulaProperty) {
            ObjectFormulaProperty ObjectProperty = (ObjectFormulaProperty) implementations.property;
            // сначала кидаем на baseClass, bit'ы
            for(InterfaceClass<JoinPropertyInterface> ValueInterface : getMapClassSet(Read, (InterfaceClass<T>) ObjectProperty.getInterfaceClass(ClassSet.getUp(DataClass.base)))) {
                InterfaceClass<T> ImplementInterface = new InterfaceClass<T>();
                for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> MapInterface : implementations.mapping.entrySet()) // если null то уже не подходит по интерфейсу
                    ImplementInterface.put(MapInterface.getKey(), MapInterface.getValue().mapValueClass(ValueInterface));

                if(!ImplementInterface.isEmpty()) {
                    Result.or(new ChangeClass<JoinPropertyInterface>(ValueInterface, implementations.property.getValueClass(ImplementInterface)));
                    MapClasses.or(ImplementInterface);
                }
            }
        } else {
            for(ChangeClass<T> ImplementChange : Read.getMapChangeClass(implementations.property))
                for(InterfaceClass<T> ImplementInterface : ImplementChange.interfaceClasses) {
                    InterfaceClassSet<JoinPropertyInterface> ResultInterface = getMapClassSet(Read, ImplementInterface);
                    if(!ResultInterface.isEmpty()) {
                        Result.or(new ChangeClass<JoinPropertyInterface>(ResultInterface,ImplementChange.value));
                        MapClasses.or(ImplementInterface);
                    }
                }
        }

        return Result;
    }

    void fillChangedRead(UnionQuery<JoinPropertyInterface, PropertyField> listQuery, PropertyField value, MapChangedRead<JoinPropertyInterface> read, ValueClassSet<JoinPropertyInterface> readClasses) {
        // создается JoinQuery - на вход getMapInterfaces, Query.MapKeys - map интерфейсов
        InterfaceClassSet<T> MapClasses = new InterfaceClassSet<T>();
        ValueClassSet<JoinPropertyInterface> result = getReadValueClassSet(read, MapClasses);
        if(result.isEmpty()) return;

        JoinQuery<JoinPropertyInterface,PropertyField> query = new JoinQuery<JoinPropertyInterface, PropertyField>(interfaces);

        // далее создается для getMapImplements - map <ImplementClass,SourceExpr> имплементаций - по getExpr'ы (Changed,SourceExpr) с переданным map интерфейсов
        Map<T, SourceExpr> implementSources = new HashMap<T,SourceExpr>();
        Map<PropertyInterfaceImplement<JoinPropertyInterface>, SourceExpr> implementExprs = new HashMap<PropertyInterfaceImplement<JoinPropertyInterface>, SourceExpr>();
        for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> implement : implementations.mapping.entrySet()) {
            SourceExpr implementExpr = read.getImplementExpr(implement.getValue(), query.mapKeys, result.getClassSet(ClassSet.universal));
            implementSources.put(implement.getKey(), implementExpr);
            implementExprs.put(implement.getValue(), implementExpr);
        }
        read.fillMapExpr(query, value, implementations.property,implementSources,implementExprs,MapClasses);

        readClasses.or(result);
        listQuery.add(query);
    }

    public ClassSet calculateValueClass(InterfaceClass<JoinPropertyInterface> ClassImplement) {
        return getValueClassSet().getValueClass(ClassImplement);
    }

    public InterfaceClassSet<JoinPropertyInterface> calculateClassSet(ClassSet reqValue) {
        InterfaceClassSet<JoinPropertyInterface> Result = new InterfaceClassSet<JoinPropertyInterface>();
        for(InterfaceClass<T> ImplementClass : implementations.property.getClassSet(reqValue))
            Result.or(getMapClassSet(new MapRead<JoinPropertyInterface>(),ImplementClass));
        return Result;
    }

    public ValueClassSet<JoinPropertyInterface> calculateValueClassSet() {
        return getReadValueClassSet(DBRead,new InterfaceClassSet<T>());
    }

    Object JoinValue = "jvalue";
    SourceExpr calculateSourceExpr(Map<JoinPropertyInterface,? extends SourceExpr> joinImplement, InterfaceClassSet<JoinPropertyInterface> joinClasses) {

        // именно через Query потому как если не хватит ключей компилятор их подхватит здесь
        JoinQuery<JoinPropertyInterface,Object> Query = new JoinQuery<JoinPropertyInterface,Object>(interfaces);
        // считаем новые SourceExpr'ы и классы
        Map<T, SourceExpr> ImplementSources = new HashMap<T, SourceExpr>();
        for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> Implement : implementations.mapping.entrySet())
            ImplementSources.put(Implement.getKey(),Implement.getValue().mapSourceExpr(Query.mapKeys, joinClasses));

        InterfaceClassSet<T> ImplementClasses = new InterfaceClassSet<T>();
        for(InterfaceClass<JoinPropertyInterface> JoinClass : joinClasses) {
            InterfaceClass<T> ImplementClass = new InterfaceClass<T>();
            for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> Implement : implementations.mapping.entrySet())
                ImplementClass.put(Implement.getKey(),Implement.getValue().mapValueClass(JoinClass));
            ImplementClasses.or(ImplementClass);
        }

        Query.properties.put(JoinValue, implementations.property.getSourceExpr(ImplementSources, ImplementClasses));
        return (new Join<JoinPropertyInterface,Object>(Query, joinImplement)).exprs.get(JoinValue);
    }

    Map<PropertyField, Type> getMapNullProps(PropertyField Value) {
        Map<PropertyField, Type> NullProps = new HashMap<PropertyField, Type>();
        NullProps.put(Value, getType());
        return NullProps;
    }

    PropertyField getDefaultObject() {
        return changeTable.value;
    }

    List<PropertyMapImplement<PropertyInterface, JoinPropertyInterface>> getImplements(Map<JoinPropertyInterface, ObjectValue> Keys, ChangePropertySecurityPolicy securityPolicy) {
        List<PropertyMapImplement<PropertyInterface, JoinPropertyInterface>> Result = new ArrayList<PropertyMapImplement<PropertyInterface, JoinPropertyInterface>>();
        List<PropertyMapImplement<PropertyInterface, JoinPropertyInterface>> BitProps = new ArrayList<PropertyMapImplement<PropertyInterface, JoinPropertyInterface>>();
        for(PropertyInterfaceImplement<JoinPropertyInterface> Implement : implementations.mapping.values())
            if(Implement instanceof PropertyMapImplement) {
                PropertyMapImplement<PropertyInterface, JoinPropertyInterface> PropertyImplement = (PropertyMapImplement<PropertyInterface, JoinPropertyInterface>)Implement;
                // все Data вперед, биты назад, остальные попорядку
                if(PropertyImplement.property instanceof DataProperty)
                    Result.add(PropertyImplement);
                else {
                    ChangeValue ChangeValue = PropertyImplement.mapGetChangeProperty(null, Keys, 0, securityPolicy);
                    if(ChangeValue!=null && ChangeValue.Class instanceof BitClass)
                        BitProps.add(PropertyImplement);
                    else // в начало
                        Result.add(0,PropertyImplement);
                }
            }
        Result.addAll(0,BitProps);
        return Result;
    }
}
