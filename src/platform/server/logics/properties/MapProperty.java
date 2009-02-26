package platform.server.logics.properties;

import platform.base.SetBuilder;
import platform.server.data.query.*;
import platform.server.data.types.Type;
import platform.server.logics.classes.sets.ValueClassSet;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataChanges;
import platform.server.logics.session.DataSession;

import java.util.*;

// ObjectMapClass = PropertyField - Join, Object - Group
abstract class MapProperty<T extends PropertyInterface,M extends PropertyInterface,IN extends PropertyInterface,IM extends PropertyInterface,OM> extends AggregateProperty<T> {

    MapProperty(TableFactory iTableFactory) {
        super(iTableFactory);
        DBRead = new MapRead<IN>();
    }

    // получает св-во для Map'а
    // Join - return Implements.Property
    // Group - return GroupProperty
    abstract Property<M> getMapProperty();

    // получает список имплементаций
    // Join - return Implements.Mapping
    // Group бежит по GroupPropertyInterface и возвращает сформированный Map
    abstract Map<IM,PropertyInterfaceImplement<IN>> getMapImplements();

    // получает список интерфейсов
    // Join - return Interfaces
    // Group - return GroupProperty.Interfaces
    abstract Collection<IN> getMapInterfaces();

    abstract void fillChangedRead(UnionQuery<IN, OM> listQuery, OM value, MapChangedRead<IN> read, ValueClassSet<T> readClasses);

    // получает св-ва для запроса
    abstract Map<OM, Type> getMapNullProps(OM Value);

    // ВЫПОЛНЕНИЕ СПИСКА ИТЕРАЦИЙ

    JoinQuery<IN, OM> getMapQuery(List<MapChangedRead<IN>> readList, OM value, ValueClassSet<T> readClass, boolean sum) {

        // делаем getQuery для всех итераций, после чего Query делаем Union на 3, InterfaceClassSet на AND(*), Value на AND(*)
        UnionQuery<IN, OM> ListQuery = sum?new OperationQuery<IN, OM>(getMapInterfaces(), Union.SUM):new ChangeQuery<IN, OM>(getMapInterfaces());
        for(MapChangedRead<IN> Read : readList)
            if(Read.check(getMapProperty()))
                fillChangedRead(ListQuery, value, Read, readClass);
        if(ListQuery.where.isFalse())
            for(Map.Entry<OM,Type> nullProp : getMapNullProps(value).entrySet())
                ListQuery.properties.put(nullProp.getKey(), nullProp.getValue().getExpr(null));

        return ListQuery;
    }

    // get* получают списки итераций чтобы потом отправить их на выполнение:

    List<MapChangedRead<IN>> getImplementSet(DataSession Session, List<PropertyInterfaceImplement<IN>> SubSet, int ImplementType, boolean NotNull) {
        List<MapChangedRead<IN>> Result = new ArrayList<MapChangedRead<IN>>();
        if(ImplementType==2) // DEBUG
            throw new RuntimeException("по идее не должно быть");
        if(!(NotNull && SubSet.size()==1)) { // сначала "зануляем" ( пропускаем NotNull только одной размерности, теоретически можно доказать(
            if(implementAllInterfaces()) // просто без Join'a
                Result.add(new MapChangedRead<IN>(Session, false, 1, ImplementType, SubSet));
            else {
                Result.add(new MapChangedRead<IN>(Session, true, 3, 2, SubSet));
                Result.add(new MapChangedRead<IN>(Session, false, 2, 2, SubSet));
            }
        }
        // затем Join'им
        Result.add(new MapChangedRead<IN>(Session, false, 0, ImplementType, SubSet));

        return Result;
    }

    // новое состояние
    List<MapChangedRead<IN>> getChange(DataSession Session,int MapType) {
        List<MapChangedRead<IN>> ChangedList = new ArrayList<MapChangedRead<IN>>();
        for(List<PropertyInterfaceImplement<IN>> SubSet : SetBuilder.buildSubSetList(getMapImplements().values())) {
            if(SubSet.size()>0)
                ChangedList.addAll(getImplementSet(Session, SubSet, 0, false));
            ChangedList.add(new MapChangedRead<IN>(Session,true,MapType,0,SubSet));
        }
        return ChangedList;
    }

    // новое состояние с измененным основным значением
    // J - C (0,1) - SS+ (0)
    List<MapChangedRead<IN>> getChangeMap(DataSession Session, int MapType) {
        List<MapChangedRead<IN>> ChangedList = new ArrayList<MapChangedRead<IN>>();
        for(List<PropertyInterfaceImplement<IN>> SubSet : SetBuilder.buildSubSetList(getMapImplements().values()))
            ChangedList.add(new MapChangedRead<IN>(Session,true,MapType,0,SubSet));
        return ChangedList;
    }
    // новое значение для имплементаций, здесь если не все имплементации придется извращаться и exclude'ать все не измененные выражения
    // LJ - P - SS (0,1)
    List<MapChangedRead<IN>> getChangeImplements(DataSession Session,int ImplementType) {
        List<MapChangedRead<IN>> ChangedList = new ArrayList<MapChangedRead<IN>>();
        for(List<PropertyInterfaceImplement<IN>> SubSet : SetBuilder.buildSubSetList(getMapImplements().values()))
            if(SubSet.size()>0)
                ChangedList.addAll(getImplementSet(Session, SubSet, ImplementType, true));

        return ChangedList;
    }
    // предыдущие значения по измененным объектам
    // J - P - L(2)
    List<MapChangedRead<IN>> getPreviousImplements(DataSession Session) {
        List<MapChangedRead<IN>> ChangedList = new ArrayList<MapChangedRead<IN>>();
        for(PropertyInterfaceImplement<IN> Implement : getMapImplements().values())
            ChangedList.add(new MapChangedRead<IN>(Session,false,0,2,Implement));
        return ChangedList;
    }
    // предыдущие значения
    List<MapChangedRead<IN>> getPreviousChange(DataSession Session) {
        List<MapChangedRead<IN>> ChangedList = getPreviousImplements(Session);
        ChangedList.add(new MapChangedRead<IN>(Session,true,2,0,new ArrayList<PropertyInterfaceImplement<IN>>()));
        return ChangedList;
    }
    // чтобы можно было бы использовать в одном списке
    MapChangedRead<IN> getPrevious(DataSession Session) {
        return new MapChangedRead<IN>(Session,false,0,0,new ArrayList<PropertyInterfaceImplement<IN>>());
    }
    // значение из базы (можно и LJ)
    // J - P - P
    MapRead<IN> DBRead;

    // получает источник для данных
/*    abstract OM getDefaultObject();
    abstract Source<T,OM> getMapSourceQuery(OM Value);

    SourceExpr calculateSourceExpr(Map<T, SourceExpr> JoinImplement, InterfaceClassSet<T> JoinClasses, boolean NotNull) {
        OM Value = getDefaultObject();
        return (new Join<T,OM>(getMapSourceQuery(Value),JoinImplement,NotNull)).Exprs.get(Value);
    }*/

    // заполняет список, возвращает есть ли изменения
    public boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate) {
        if(ChangedProperties.contains(this)) return true;
        if(NoUpdate.contains(this)) return false;

        boolean Changed = getMapProperty().fillChangedList(ChangedProperties, Changes, NoUpdate);

        for(PropertyInterfaceImplement Implement : getMapImplements().values())
            Changed = Implement.mapFillChangedList(ChangedProperties, Changes, NoUpdate) || Changed;

        if(Changed)
            ChangedProperties.add(this);

        return Changed;
    }

    boolean containsImplement(Collection<Property> Properties) {
        for(PropertyInterfaceImplement Implement : getMapImplements().values())
            if(Implement instanceof PropertyMapImplement && Properties.contains(((PropertyMapImplement)Implement).property))
                return true;
        return false;
    }

    boolean implementAllInterfaces() {

        if(getMapProperty() instanceof WhereFormulaProperty) return false;

        Set<PropertyInterface> ImplementInterfaces = new HashSet<PropertyInterface>();
        for(PropertyInterfaceImplement<IN> InterfaceImplement : getMapImplements().values()) {
            if(InterfaceImplement instanceof PropertyMapImplement)
                ImplementInterfaces.addAll(((PropertyMapImplement<?,IN>)InterfaceImplement).mapping.values());
        }

        return ImplementInterfaces.size()==getMapInterfaces().size();
    }
}
