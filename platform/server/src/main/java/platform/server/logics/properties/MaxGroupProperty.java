package platform.server.logics.properties;

import platform.interop.Compare;
import platform.server.data.KeyField;
import platform.server.data.ModifyQuery;
import platform.server.data.PropertyField;
import platform.server.data.query.ChangeQuery;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.data.types.Type;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.ValueClassSet;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class MaxGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    public MaxGroupProperty(String iSID, Collection<GroupPropertyInterface<T>> iInterfaces, TableFactory iTableFactory, Property<T> iProperty) {
        super(iSID, iInterfaces, iTableFactory, iProperty, 0);
    }

    public void fillRequiredChanges(Integer incrementType, Map<Property, Integer> requiredTypes) {
        // Group на ->2, Interface на ->2 - как было - возвр. 2
        groupProperty.setChangeType(requiredTypes,2);

        for(GroupPropertyInterface<T> Interface : interfaces)
            if(Interface.implement instanceof PropertyMapImplement)
                ((PropertyMapImplement)Interface.implement).property.setChangeType(requiredTypes,2);
    }

    public Change incrementChanges(DataSession session, int changeType) throws SQLException {

        // делаем Full Join (на 3) :
        //      a) ушедшие (previmp и prevmap) = старые (sourceexpr) LJ (prev+change) (вообще и пришедшие <= старых)
        //      b) пришедшие (change) > старых (sourceexpr)

        ValueClassSet<GroupPropertyInterface<T>> resultClass = new ValueClassSet<GroupPropertyInterface<T>>();

        PropertyField prevMapValue = new PropertyField("drop", Type.integer);

        ChangeQuery<GroupPropertyInterface<T>, PropertyField> changeQuery = new ChangeQuery<GroupPropertyInterface<T>, PropertyField>(interfaces);

        changeQuery.add(getGroupQuery(getPreviousChange(session),prevMapValue,resultClass));
        changeQuery.add(getGroupQuery(getChange(session,0), changeTable.value,resultClass));

        // подозрительные на изменения ключи
        JoinQuery<GroupPropertyInterface<T>,PropertyField> suspiciousQuery = new JoinQuery<GroupPropertyInterface<T>, PropertyField>(interfaces);
        Join<GroupPropertyInterface<T>, PropertyField> changeJoin = new Join<GroupPropertyInterface<T>, PropertyField>(changeQuery,suspiciousQuery);

        JoinExpr newValue = changeJoin.exprs.get(changeTable.value);
        JoinExpr OldValue = changeJoin.exprs.get(prevMapValue);
        SourceExpr prevValue = getSourceExpr(suspiciousQuery.mapKeys,resultClass.getClassSet(ClassSet.universal));

        suspiciousQuery.properties.put(changeTable.value, newValue);
        suspiciousQuery.properties.put(changeTable.prevValue, prevValue);

        suspiciousQuery.and(newValue.getWhere().and(prevValue.getWhere().not()).or(
                new CompareWhere(prevValue,newValue, Compare.LESS)).or(new CompareWhere(prevValue,OldValue, Compare.EQUALS)));


        // сохраняем
        Change incrementChange = new Change(2,suspiciousQuery,resultClass);
        incrementChange.save(session);

        JoinQuery<GroupPropertyInterface<T>, PropertyField> reReadQuery = new JoinQuery<GroupPropertyInterface<T>, PropertyField>(interfaces);
        Join<GroupPropertyInterface<T>, PropertyField> sourceJoin = new Join<GroupPropertyInterface<T>, PropertyField>(incrementChange.source, reReadQuery);

        newValue = sourceJoin.exprs.get(changeTable.value);
        // новое null и InJoin или ноаое меньше старого
        reReadQuery.and(sourceJoin.inJoin.and(newValue.getWhere().not()).or(new CompareWhere(newValue,sourceJoin.exprs.get(changeTable.prevValue), Compare.LESS)));

        if(!(reReadQuery.executeSelect(session,new LinkedHashMap<PropertyField,Boolean>(),1).size() == 0)) {
            // если кол-во > 0 перечитываем, делаем LJ GQ с протолкнутым ReReadQuery
            JoinQuery<KeyField, PropertyField> updateQuery = new JoinQuery<KeyField, PropertyField>(changeTable.keys);
            updateQuery.putKeyWhere(Collections.singletonMap(changeTable.property,ID));
            // сначала на LJ чтобы заNULL'ить максимумы
            updateQuery.and(new Join<GroupPropertyInterface<T>, PropertyField>(reReadQuery, changeTableMap, updateQuery).inJoin);
            // затем новые значения
            ValueClassSet<GroupPropertyInterface<T>> newClass = new ValueClassSet<GroupPropertyInterface<T>>();
            List<MapChangedRead<T>> newRead = new ArrayList<MapChangedRead<T>>(); newRead.add(getPrevious(session)); newRead.addAll(getChange(session,0));
            updateQuery.properties.put(changeTable.value, (new Join<GroupPropertyInterface<T>, PropertyField>(getGroupQuery(newRead, changeTable.value,newClass), changeTableMap, updateQuery)).exprs.get(changeTable.value));

//            Main.Session = Session;
//            new ModifyQuery(ChangeTable,UpdateQuery).outSelect(Session);
//            Main.Session = null;
            resultClass.or(resultClass.and(newClass));
            session.updateRecords(new ModifyQuery(changeTable, updateQuery));
        }
        return incrementChange;
    }

    public Integer getIncrementType(Collection<Property> changedProps, Set<Property> toWait) {
        return 2;
    }
}
