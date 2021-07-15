package lsfusion.server.physics.exec.db.table;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.query.modify.ModifyQuery;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.TableStatKeys;
import lsfusion.server.data.table.Field;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.table.TableOwner;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.SystemClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;

// таблица счетчика sID
public class IDTable extends DBTable {

    public final static IntegerClass idTypeClass = IntegerClass.instance;

    public static final IDTable instance = new IDTable(); 

    KeyField key;
    PropertyField value;

    public IDTable() {
        super("idtable");
        key = new KeyField("id", idTypeClass);
        keys = SetFact.singletonOrder(key);

        value = new PropertyField("value", SystemClass.instance);
        properties = SetFact.singleton(value);

        classes = new ClassWhere<>(key, idTypeClass);

        ImMap<Field, DataClass> valueClasses = MapFact.toMap(key, idTypeClass, value, SystemClass.instance);
        propertyClasses = MapFact.singleton(value, new ClassWhere<>(valueClasses));
    }

    public final static int OBJECT = 1;
    public final static int FORM = 2;
    public final static int NAME = 3;

    static ImMap<Integer, Long> getCounters() {
        return MapFact.toMap(OBJECT, 10L, FORM, 0L); // потому как есть базовый пул 0,1,2 предопределенных ID'ков
    }

    @IdentityInstanceLazy
    private Query<KeyField, PropertyField> getGenerateQuery(int idType) {
        QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<>(this, MapFact.singleton(key, new DataObject(idType, idTypeClass)));
        lsfusion.server.data.query.build.Join<PropertyField> joinTable = join(query.getMapExprs());
        query.and(joinTable.getWhere());
        query.addProperty(value, joinTable.getExpr(value));
        return query.getQuery();
    }

    private MAddMap<Integer, Pair<Long, Long>> ids = MapFact.mAddMap(MapFact.override());
    {
        ImMap<Integer, Long> counters = getCounters();
        for(int i=0,size=counters.size();i<size;i++)
            ids.add(counters.getKey(i), new Pair<>(0L, -1L));
    }
    
    public long generateID(SQLSession dataSession, int idType) throws SQLException {

        Long result;
        synchronized (this) {
            assert !dataSession.isInTransaction();

            Pair<Long, Long> id = ids.get(idType);
            long freeID = id.first;
            long maxReservedID = id.second;

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

    public Pair<Long, Long>[] generateIDs(long count, SQLSession dataSession, int idType) throws SQLException {
        synchronized (this) {
            Pair<Long, Long> id = ids.get(idType);
            long freeID = id.first;
            long maxReservedID = id.second;

            Pair<Long, Long> fromPoolIDs;
            long fromPool = maxReservedID - freeID + 1;
            if(fromPool >= count) {
                fromPoolIDs = new Pair<>(freeID, count);
                freeID += count;

                ids.add(idType, new Pair<>(freeID, maxReservedID));
                return new Pair[]{fromPoolIDs};
            } else
                fromPoolIDs = new Pair<>(freeID, fromPool);

            long rest = count - fromPool;
            assert rest > 0;

            long newReserved = rest + Settings.get().getReserveIDStep();
            long newID = reserveIDs(newReserved, dataSession, idType);
            Pair<Long, Long> genPoolIDs = new Pair<>(newID, rest);

            maxReservedID = newID + newReserved - 1;
            freeID = newID + rest;

            ids.add(idType, new Pair<>(freeID, maxReservedID));
            if(fromPool > 0)
                return new Pair[] {fromPoolIDs, genPoolIDs};
            return new Pair[] {genPoolIDs};
        }
    }

    public long reserveIDs(long count, SQLSession dataSession, int idType) throws SQLException {
        return reserveIDs(count, dataSession, idType, 0);
    }

        // возвращает первый, и резервирует себе еще count id'ков
    private long reserveIDs(long count, SQLSession dataSession, int idType, int attempts) throws SQLException {
        long freeID;
        try {
            dataSession.startTransaction(DBManager.getTIL(), OperationOwner.unknown);

            freeID = ObjectType.idClass.read(getGenerateQuery(idType).execute(dataSession, OperationOwner.unknown).singleValue().get(value)) + 1; // замещаем

            QueryBuilder<KeyField, PropertyField> updateQuery = new QueryBuilder<>(this, MapFact.singleton(key, new DataObject(idType, idTypeClass)));
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

    public TableStatKeys getTableStatKeys() {
        return getStatKeys(this, getCounters().size());
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        return getStatProps(this);
    }
}
