package lsfusion.server.logics.table;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.classes.SystemClass;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.DataObject;

import java.sql.SQLException;

// таблица счетчика sID
public class IDTable extends GlobalTable {

    public static final IDTable instance = new IDTable(); 

    KeyField key;
    PropertyField value;

    public IDTable() {
        super("idtable");
        key = new KeyField("id", SystemClass.instance);
        keys = SetFact.singletonOrder(key);

        value = new PropertyField("value", SystemClass.instance);
        properties = SetFact.singleton(value);

        classes = new ClassWhere<>(key, SystemClass.instance);

        ImMap<Field, SystemClass> valueClasses = MapFact.toMap(key, SystemClass.instance, value, SystemClass.instance);
        propertyClasses = MapFact.singleton(value, new ClassWhere<>(valueClasses));
    }

    public final static int OBJECT = 1;
    public final static int FORM = 2;
    public final static int NAME = 3;

    static ImMap<Integer, Integer> getCounters() {
        return MapFact.toMap(OBJECT, 10, FORM, 0); // потому как есть базовый пул 0,1,2 предопределенных ID'ков
    }

    @IdentityInstanceLazy
    private Query<KeyField, PropertyField> getGenerateQuery(int idType) {
        QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<>(this, MapFact.singleton(key, new DataObject(idType, SystemClass.instance)));
        lsfusion.server.data.query.Join<PropertyField> joinTable = join(query.getMapExprs());
        query.and(joinTable.getWhere());
        query.addProperty(value, joinTable.getExpr(value));
        return query.getQuery();
    }

    private MAddMap<Integer, Pair<Integer, Integer>> ids = MapFact.mAddMap(MapFact.<Integer, Pair<Integer, Integer>>override());
    {
        ImMap<Integer, Integer> counters = getCounters();
        for(int i=0,size=counters.size();i<size;i++)
            ids.add(counters.getKey(i), new Pair<>(0, -1));
    }
    
    public int generateID(SQLSession dataSession, int idType) throws SQLException {

        Integer result;
        synchronized (this) {
            assert !dataSession.isInTransaction();

            Pair<Integer, Integer> id = ids.get(idType);
            int freeID = id.first;
            int maxReservedID = id.second;

            if(freeID > maxReservedID) { // читаем новый пул
                int reserveIDStep = Settings.get().getReserveIDStep();
                freeID = reserveIDs(reserveIDStep, dataSession, idType);
                maxReservedID = freeID + reserveIDStep - 1;
            }
            result = freeID++;

            ids.add(idType, new Pair<>(freeID, maxReservedID));
        }

        return result;
    }

    public Pair<Integer, Integer>[] generateIDs(int count, SQLSession dataSession, int idType) throws SQLException {
        synchronized (this) {
            Pair<Integer, Integer> id = ids.get(idType);
            int freeID = id.first;
            int maxReservedID = id.second;

            Pair<Integer, Integer> fromPoolIDs;
            int fromPool = maxReservedID - freeID + 1;
            if(fromPool >= count) {
                fromPoolIDs = new Pair<>(freeID, count);
                freeID += count;

                ids.add(idType, new Pair<>(freeID, maxReservedID));
                return new Pair[]{fromPoolIDs};
            } else
                fromPoolIDs = new Pair<>(freeID, fromPool);

            int rest = count - fromPool;
            assert rest > 0;

            int newReserved = rest + Settings.get().getReserveIDStep();
            int newID = reserveIDs(newReserved, dataSession, idType);
            Pair<Integer, Integer> genPoolIDs = new Pair<>(newID, rest);

            maxReservedID = newID + newReserved - 1;
            freeID = newID + rest;

            ids.add(idType, new Pair<>(freeID, maxReservedID));
            if(fromPool > 0)
                return new Pair[] {fromPoolIDs, genPoolIDs};
            return new Pair[] {genPoolIDs};
        }
    }

    public int reserveIDs(int count, SQLSession dataSession, int idType) throws SQLException {
        return reserveIDs(count, dataSession, idType, 0);
    }

        // возвращает первый, и резервирует себе еще count id'ков
    private int reserveIDs(int count, SQLSession dataSession, int idType, int attempts) throws SQLException {
        int freeID = 0;
        try {
            dataSession.startTransaction(DBManager.ID_TIL, OperationOwner.unknown);

            freeID = (Integer) getGenerateQuery(idType).execute(dataSession, OperationOwner.unknown).singleValue().get(value) + 1; // замещаем

            QueryBuilder<KeyField, PropertyField> updateQuery = new QueryBuilder<>(this, MapFact.singleton(key, new DataObject(idType, SystemClass.instance)));
            updateQuery.addProperty(value, new ValueExpr(freeID + count - 1, SystemClass.instance));
            dataSession.updateRecords(new ModifyQuery(this, updateQuery.getQuery(), OperationOwner.unknown, TableOwner.global));

            dataSession.commitTransaction();
        } catch (Throwable e) {
            ServerLoggers.sqlSuppLog(e); // just in case if rollback will fail

            dataSession.rollbackTransaction();

            if(e instanceof SQLHandledException && ((SQLHandledException)e).repeatApply(dataSession, OperationOwner.unknown, attempts)) // update conflict или deadlock или timeout - пробуем еще раз
                return reserveIDs(count, dataSession, idType, attempts + 1);
            
            throw ExceptionUtils.propagate(e, SQLException.class);
        }
        return freeID;
    }

    public StatKeys<KeyField> getStatKeys() {
        return getStatKeys(this, getCounters().size());
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        throw new RuntimeException("not supported");
    }
}
