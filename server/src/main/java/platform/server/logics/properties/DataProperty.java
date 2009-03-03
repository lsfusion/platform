package platform.server.logics.properties;

import platform.interop.Compare;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.Table;
import platform.server.data.query.ChangeQuery;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.logics.ObjectValue;
import platform.server.logics.auth.ChangePropertySecurityPolicy;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.sets.*;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.*;

import java.sql.SQLException;
import java.util.*;

public class DataProperty<D extends PropertyInterface> extends Property<DataPropertyInterface> {
    public RemoteClass value;

    public DataProperty(TableFactory iTableFactory, RemoteClass iValue) {
        super(iTableFactory);
        value = iValue;

        defaultMap = new HashMap<DataPropertyInterface,D>();
    }

    // при текущей реализации проше предполагать что не имплементнутые Interface имеют null Select !!!!!
    public Table getTable(Map<KeyField,DataPropertyInterface> MapJoins) {
        return tableFactory.getTable(interfaces,MapJoins);
    }

    public ClassSet calculateValueClass(InterfaceClass<DataPropertyInterface> ClassImplement) {
        // пока так потом сделаем перегрузку по классам
        // если не тот класс сразу зарубаем
       for(DataPropertyInterface DataInterface : interfaces)
            if(!ClassImplement.get(DataInterface).intersect(ClassSet.getUp(DataInterface.interfaceClass)))
                return new ClassSet();

        return ClassSet.getUp(value);
    }

    public InterfaceClassSet<DataPropertyInterface> calculateClassSet(ClassSet reqValue) {
        if(reqValue.intersect(ClassSet.getUp(value))) {
            InterfaceClass<DataPropertyInterface> ResultInterface = new InterfaceClass<DataPropertyInterface>();
            for(DataPropertyInterface Interface : interfaces)
                ResultInterface.put(Interface,ClassSet.getUp(Interface.interfaceClass));
            return new InterfaceClassSet<DataPropertyInterface>(ResultInterface);
        } else
            return new InterfaceClassSet<DataPropertyInterface>();
    }

    public ValueClassSet<DataPropertyInterface> calculateValueClassSet() {
        return new ValueClassSet<DataPropertyInterface>(ClassSet.getUp(value),getClassSet(ClassSet.universal));
    }

    // свойства для "ручных" изменений пользователем
    public DataChangeTable dataTable;
    public Map<KeyField,DataPropertyInterface> dataTableMap = null;

    public void fillDataTable() {
        dataTable = tableFactory.getDataChangeTable(interfaces.size(), getType());
        // если нету Map'a построим
        dataTableMap = new HashMap<KeyField,DataPropertyInterface>();
        Iterator<KeyField> io = dataTable.objects.iterator();
        for(DataPropertyInterface Interface : interfaces)
            dataTableMap.put(io.next(),Interface);
    }

    void outDataChangesTable(DataSession Session) throws SQLException {
        dataTable.outSelect(Session);
    }

    public ChangeValue getChangeProperty(DataSession Session, Map<DataPropertyInterface, ObjectValue> Keys, int Coeff, ChangePropertySecurityPolicy securityPolicy) throws SQLException {

        if(!getValueClass(new InterfaceClass<DataPropertyInterface>(Keys)).isEmpty() && (securityPolicy == null || securityPolicy.checkPermission(this))) {
            if(Coeff==0 && Session!=null) {
                return new ChangeObjectValue(value, Session.readProperty(this,Keys));
            } else
                return new ChangeCoeffValue(value,Coeff);
        } else
            return null;
    }

    public void changeProperty(Map<DataPropertyInterface, ObjectValue> Keys, Object NewValue, boolean externalID, DataSession Session, ChangePropertySecurityPolicy securityPolicy) throws SQLException {
        // записываем в таблицу изменений
        if (securityPolicy == null || securityPolicy.checkPermission(this))
            Session.changeProperty(this, Keys, NewValue, externalID);
    }

    // св-во по умолчанию (при ClassSet подставляется)
    public Property<D> defaultProperty;
    // map интерфейсов на PropertyInterface
    public Map<DataPropertyInterface,D> defaultMap;
    // если нужно еще за изменениями следить и перебивать
    public boolean onDefaultChange;

    // заполняет список, возвращает есть ли изменения, последний параметр для рекурсий
    public boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate) {
        if(ChangedProperties.contains(this)) return true;
        if(NoUpdate.contains(this)) return false;
        // если null то значит полный список запрашивают
        if(Changes==null) return true;

        boolean Changed = Changes.properties.contains(this);

        if(!Changed)
            for(DataPropertyInterface Interface : interfaces)
                if(Changes.removeClasses.contains(Interface.interfaceClass)) Changed = true;

        if(!Changed)
            if(Changes.removeClasses.contains(value)) Changed = true;

        if(defaultProperty !=null) {
            boolean DefaultChanged = defaultProperty.fillChangedList(ChangedProperties, Changes, NoUpdate);
            if(!Changed) {
                if(onDefaultChange)
                    Changed = DefaultChanged;
                else
                    for(DataPropertyInterface Interface : interfaces)
                        if(Changes.addClasses.contains(Interface.interfaceClass)) Changed = true;
            }
        }

        if(Changed) {
            ChangedProperties.add(this);
            return true;
        } else
            return false;
    }

    public void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {

        // если на изм. надо предыдущее изменение иначе просто на =
        // пока неясно после реализации QueryIncrementChanged станет яснее
        if(defaultProperty !=null && RequiredTypes.containsKey(defaultProperty))
            defaultProperty.setChangeType(RequiredTypes, onDefaultChange ?2:0);
    }

    // заполним старыми значениями (LEFT JOIN'ом)
    public Change incrementChanges(DataSession session, int changeType) {

        // на 3 то есть слева/направо
        ChangeQuery<DataPropertyInterface, PropertyField> resultQuery = new ChangeQuery<DataPropertyInterface, PropertyField>(interfaces);
        ValueClassSet<DataPropertyInterface> resultClass = new ValueClassSet<DataPropertyInterface>();

        // Default изменения (пока Add)
        if(defaultProperty !=null) {
            if(!onDefaultChange) {
                // бежим по всем добавленным интерфейсам
                for(DataPropertyInterface propertyInterface : interfaces)
                    if(session.changes.addClasses.contains(propertyInterface.interfaceClass)) {
                        JoinQuery<DataPropertyInterface,PropertyField> query = new JoinQuery<DataPropertyInterface, PropertyField>(interfaces);
                        Map<D, SourceExpr> joinImplement = new HashMap<D, SourceExpr>();
                        // "перекодируем" в базовый интерфейс
                        for(DataPropertyInterface dataInterface : interfaces)
                            joinImplement.put(defaultMap.get(dataInterface),query.mapKeys.get(dataInterface));

                        // вкидываем "новое" состояние DefaultProperty с Join'ое с AddClassTable
                        // если DefaultProperty требует на входе такой добавляемый интерфейс то можно чисто новое брать
                        joinChangeClass(tableFactory.addClassTable,query,session,propertyInterface);

                        ValueClassSet<D> defaultValueSet = session.getSourceClass(defaultProperty).and(new ChangeClass<D>(defaultMap.get(propertyInterface),session.addChanges.get(propertyInterface.interfaceClass)));
                        SourceExpr defaultExpr = session.getSourceExpr(defaultProperty, joinImplement, defaultValueSet.getClassSet(ClassSet.universal));
                        query.properties.put(changeTable.value, defaultExpr);
                        query.and(defaultExpr.getWhere());

                        resultQuery.add(query);
                        resultClass.or(defaultValueSet.mapBack(defaultMap));
                    }
            } else {
                if(session.propertyChanges.containsKey(defaultProperty)) {
                    Property<D>.Change defaultChange = session.getChange(defaultProperty);
                    JoinQuery<DataPropertyInterface, PropertyField> query = new JoinQuery<DataPropertyInterface, PropertyField>(interfaces);

                    Map<D, SourceExpr> joinImplement = new HashMap<D, SourceExpr>();
                    // "перекодируем" в базовый интерфейс
                    for(DataPropertyInterface dataInterface : interfaces)
                        joinImplement.put(defaultMap.get(dataInterface),query.mapKeys.get(dataInterface));

                    // по изменению св-ва
                    JoinExpr newExpr = defaultChange.getExpr(joinImplement,0);
                    query.properties.put(changeTable.value, newExpr);
                    // new, не равно prev
                    query.and(newExpr.from.inJoin);
                    query.and(new CompareWhere(newExpr,defaultChange.getExpr(joinImplement,2), Compare.EQUALS).not());

                    resultQuery.add(query);
                    resultClass.or(defaultChange.classes.mapBack(defaultMap));
                }
            }
        }

        boolean dataChanged = session.changes.properties.contains(this);
        JoinQuery<DataPropertyInterface, PropertyField> dataQuery = null;
        SourceExpr dataExpr = null;
        if(dataChanged) {
            dataQuery = new JoinQuery<DataPropertyInterface, PropertyField>(interfaces);
            // GetChangedFrom
            Join<KeyField, PropertyField> dataJoin = new Join<KeyField, PropertyField>(dataTable, dataTableMap,dataQuery);
            dataJoin.joins.put(dataTable.property,dataTable.property.type.getExpr(ID));
            dataQuery.and(dataJoin.inJoin);

            dataExpr = dataJoin.exprs.get(dataTable.value);
            dataQuery.properties.put(changeTable.value, dataExpr);
            resultClass.or(session.dataChanges.get(this));
        }

        for(DataPropertyInterface removeInterface : interfaces) {
            if(session.changes.removeClasses.contains(removeInterface.interfaceClass)) {
                // те изменения которые были на удаляемые объекты исключаем
                if(dataChanged) tableFactory.removeClassTable.excludeJoin(dataQuery,session,removeInterface.interfaceClass,dataQuery.mapKeys.get(removeInterface));

                // проверяем может кто удалился из интерфейса объекта
                JoinQuery<DataPropertyInterface, PropertyField> query = new JoinQuery<DataPropertyInterface, PropertyField>(interfaces);
                joinChangeClass(tableFactory.removeClassTable,query,session,removeInterface);

                InterfaceClassSet<DataPropertyInterface> removeClassSet = getClassSet(ClassSet.universal).and(new InterfaceClass<DataPropertyInterface>(removeInterface,session.removeChanges.get(removeInterface.interfaceClass)));
                // пока сделаем что наплевать на старое значение хотя конечно 2 раза может тоже не имеет смысл считать
                query.properties.put(changeTable.value, changeTable.value.type.getExpr(null));
                query.and(getSourceExpr(query.mapKeys,removeClassSet).getWhere());

                resultQuery.add(query);
                resultClass.or(new ValueClassSet<DataPropertyInterface>(new ClassSet(),removeClassSet));
            }
        }

        if(session.changes.removeClasses.contains(value)) {
            // те изменения которые были на удаляемые объекты исключаем
            if(dataChanged) tableFactory.removeClassTable.excludeJoin(dataQuery,session, value,dataExpr);

            JoinQuery<DataPropertyInterface, PropertyField> query = new JoinQuery<DataPropertyInterface, PropertyField>(interfaces);
            Join<KeyField, PropertyField> removeJoin = new Join<KeyField, PropertyField>(tableFactory.removeClassTable.getClassJoin(session, value));

            InterfaceClassSet<DataPropertyInterface> removeClassSet = getClassSet(session.removeChanges.get(value));
            removeJoin.joins.put(tableFactory.removeClassTable.object,getSourceExpr(query.mapKeys,removeClassSet));
            query.and(removeJoin.inJoin);
            query.properties.put(changeTable.value, changeTable.value.type.getExpr(null));

            resultQuery.add(query);
            resultClass.or(new ValueClassSet<DataPropertyInterface>(new ClassSet(),removeClassSet));
        }

        // здесь именно в конце так как должна быть последней
        if(dataChanged)
            resultQuery.add(dataQuery);

        return new Change(0,resultQuery,resultClass);
    }

    public Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return 0;
    }
}
