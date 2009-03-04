package platform.server.logics.properties;

import platform.base.SetBuilder;
import platform.server.data.PropertyField;
import platform.server.data.query.ChangeQuery;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.sets.*;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataChanges;
import platform.server.logics.session.DataSession;
import platform.server.where.Where;

import java.util.*;

public class ClassProperty extends AggregateProperty<DataPropertyInterface> {

    RemoteClass valueClass;
    Object value;

    public ClassProperty(TableFactory iTableFactory, RemoteClass iValueClass, Object iValue) {
        super(iTableFactory);
        valueClass = iValueClass;
        value = iValue;
    }

    public void fillRequiredChanges(Integer incrementType, Map<Property, Integer> requiredTypes) {
        // этому св-ву чужого не надо
    }

    public boolean fillChangedList(List<Property> changedProperties, DataChanges changes, Collection<Property> noUpdate) {
        // если Value null то ничего не интересует
        if(value ==null) return false;
        if(changedProperties.contains(this)) return true;
        if(noUpdate.contains(this)) return false;

        for(DataPropertyInterface ValueInterface : interfaces)
            if(changes ==null || changes.addClasses.contains(ValueInterface.interfaceClass) || changes.removeClasses.contains(ValueInterface.interfaceClass)) {
                changedProperties.add(this);
                return true;
            }

        return false;
    }

    public Change incrementChanges(DataSession session, int changeType) {

        // работает на = и на + ему собсно пофигу, то есть сразу на 2

        // для любого изменения объекта на NEW можно определить PREV и соответственно
        // Set<Class> пришедшие, Set<Class> ушедшие
        // соответственно алгоритм бежим по всем интерфейсам делаем UnionQuery из SS изменений + старых объектов

        // конечный результат, с ключами и выражением
        ChangeQuery<DataPropertyInterface, PropertyField> resultQuery = new ChangeQuery<DataPropertyInterface, PropertyField>(interfaces);
        ValueClassSet<DataPropertyInterface> resultClass = new ValueClassSet<DataPropertyInterface>();

        List<DataPropertyInterface> removeInterfaces = new ArrayList<DataPropertyInterface>();
        for(DataPropertyInterface valueInterface : interfaces)
            if(session.changes.removeClasses.contains(valueInterface.interfaceClass))
                removeInterfaces.add(valueInterface);

        // для RemoveClass без SS все за Join'им (valueClass пока трогать не будем (так как у значения пока не закладываем механизм изменений))
        for(DataPropertyInterface changedInterface : removeInterfaces) {
            JoinQuery<DataPropertyInterface,PropertyField> query = new JoinQuery<DataPropertyInterface, PropertyField>(interfaces);
            InterfaceClass<DataPropertyInterface> removeClass = new InterfaceClass<DataPropertyInterface>();

            // RemoveClass + остальные из старой таблицы
            joinChangeClass(tableFactory.removeClassTable,query, session,changedInterface);
            removeClass.put(changedInterface, session.removeChanges.get(changedInterface.interfaceClass));
            for(DataPropertyInterface valueInterface : interfaces)
                if(valueInterface!=changedInterface) {
                    joinObjects(query,valueInterface);
                    removeClass.put(valueInterface, ClassSet.getUp(valueInterface.interfaceClass));
                }

            query.properties.put(changeTable.value, changeTable.value.type.getExpr(null));
            query.properties.put(changeTable.prevValue, changeTable.prevValue.type.getExpr(value));

            resultQuery.add(query);
            resultClass.or(new ChangeClass<DataPropertyInterface>(removeClass,new ClassSet()));
        }

        List<DataPropertyInterface> addInterfaces = new ArrayList<DataPropertyInterface>();
        for(DataPropertyInterface valueInterface : interfaces)
            if(session.changes.addClasses.contains(valueInterface.interfaceClass))
                addInterfaces.add(valueInterface);

        ListIterator<List<DataPropertyInterface>> il = SetBuilder.buildSubSetList(addInterfaces).listIterator();
        // пустое подмн-во не надо (как и в любой инкрементности)
        il.next();
        while(il.hasNext()) {
            List<DataPropertyInterface> changeProps = il.next();

            JoinQuery<DataPropertyInterface, PropertyField> query = new JoinQuery<DataPropertyInterface, PropertyField>(interfaces);
            InterfaceClass<DataPropertyInterface> addClass = new InterfaceClass<DataPropertyInterface>();

            for(DataPropertyInterface valueInterface : interfaces) {
                if(changeProps.contains(valueInterface)) {
                    joinChangeClass(tableFactory.addClassTable,query, session,valueInterface);
                    addClass.put(valueInterface, session.addChanges.get(valueInterface.interfaceClass));
                } else {
                    joinObjects(query,valueInterface);
                    addClass.put(valueInterface,ClassSet.getUp(valueInterface.interfaceClass));

                    // здесь также надо проверить что не из RemoveClasses (то есть LEFT JOIN на null)
                    if(session.changes.removeClasses.contains(valueInterface.interfaceClass))
                        tableFactory.removeClassTable.excludeJoin(query, session,valueInterface.interfaceClass,query.mapKeys.get(valueInterface));
                }
            }

            query.properties.put(changeTable.prevValue, changeTable.prevValue.type.getExpr(null));
            query.properties.put(changeTable.value, changeTable.value.type.getExpr(value));

            resultQuery.add(query);
            resultClass.or(new ChangeClass<DataPropertyInterface>(addClass,new ClassSet(valueClass)));
        }

        return new Change(2,resultQuery,resultClass);
    }

    public Integer getIncrementType(Collection<Property> changedProps, Set<Property> toWait) {
        return 0;
    }


    SourceExpr calculateSourceExpr(Map<DataPropertyInterface, ? extends SourceExpr> joinImplement, InterfaceClassSet<DataPropertyInterface> joinClasses) {

        String valueString = "value";

        JoinQuery<DataPropertyInterface,String> query = new JoinQuery<DataPropertyInterface,String>(interfaces);

        if(value ==null) // если Value null закинем false по сути в запрос
            query.and(Where.FALSE);
        else
            for(DataPropertyInterface valueInterface : interfaces)
                joinObjects(query,valueInterface);
        query.properties.put(valueString, valueClass.getType().getExpr(value));

        return (new Join<DataPropertyInterface,String>(query, joinImplement)).exprs.get(valueString);
    }

    public ClassSet calculateValueClass(InterfaceClass<DataPropertyInterface> classImplement) {
        // аналогично DataProperty\только без перегрузки классов
        for(DataPropertyInterface valueInterface : interfaces)
            if(!classImplement.get(valueInterface).intersect(ClassSet.getUp(valueInterface.interfaceClass)))
                return new ClassSet();

        return new ClassSet(valueClass);
    }

    public InterfaceClassSet<DataPropertyInterface> calculateClassSet(ClassSet reqValue) {
        // аналогично DataProperty\только без перегрузки классов
        if(reqValue.contains(valueClass)) {
            InterfaceClass<DataPropertyInterface> resultInterface = new InterfaceClass<DataPropertyInterface>();
            for(DataPropertyInterface valueInterface : interfaces)
                resultInterface.put(valueInterface,ClassSet.getUp(valueInterface.interfaceClass));
            return new InterfaceClassSet<DataPropertyInterface>(resultInterface);
        } else
            return new InterfaceClassSet<DataPropertyInterface>();
    }

    public ValueClassSet<DataPropertyInterface> calculateValueClassSet() {
        return new ValueClassSet<DataPropertyInterface>(new ClassSet(valueClass),getClassSet(ClassSet.universal));
    }
}
