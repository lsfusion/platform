package lsfusion.server.logics.table;

import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
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

        classes = new ClassWhere<KeyField>(key, SystemClass.instance);

        ImMap<Field, SystemClass> valueClasses = MapFact.toMap(key, SystemClass.instance, value, SystemClass.instance);
        propertyClasses = MapFact.singleton(value,new ClassWhere<Field>(valueClasses));
    }

    public final static int OBJECT = 1;
    public final static int FORM = 2;

    static ImMap<Integer, Integer> getCounters() {
        return MapFact.toMap(OBJECT, 10, FORM, 0); // потому как есть базовый пул 0,1,2 предопределенных ID'ков
    }

    @IdentityInstanceLazy
    private Query<KeyField, PropertyField> getGenerateQuery(int idType) {
        QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<KeyField, PropertyField>(this, MapFact.singleton(key, new DataObject(idType, SystemClass.instance)));
        lsfusion.server.data.query.Join<PropertyField> joinTable = join(query.getMapExprs());
        query.and(joinTable.getWhere());
        query.addProperty(value, joinTable.getExpr(value));
        return query.getQuery();
    }

    private int freeID = 0;
    private int maxReservedID = -1;
    public int generateID(SQLSession dataSession, int idType) throws SQLException {

        Integer result;
        synchronized (this) {
            assert !dataSession.isInTransaction();

            if(freeID > maxReservedID) { // читаем новый пул
                int reserveIDStep = Settings.get().getReserveIDStep();
                freeID = reserveIDs(reserveIDStep, dataSession, idType);
                maxReservedID = freeID + reserveIDStep - 1;
            }
            result = freeID++;
        }

        return result;
    }

    public Pair<Integer, Integer>[] generateIDs(int count, SQLSession dataSession, int idType) throws SQLException {
        synchronized (this) {
            Pair<Integer, Integer> fromPoolIDs;
            int fromPool = maxReservedID - freeID + 1;
            if(fromPool >= count) {
                fromPoolIDs = new Pair<Integer, Integer>(freeID, count);
                freeID += count;
                return new Pair[]{fromPoolIDs};
            } else
                fromPoolIDs = new Pair<Integer, Integer>(freeID, fromPool);

            int rest = count - fromPool;
            assert rest > 0;

            int newReserved = rest + Settings.get().getReserveIDStep();
            int newID = reserveIDs(newReserved, dataSession, idType);
            Pair<Integer, Integer> genPoolIDs = new Pair<Integer, Integer>(newID, rest);

            maxReservedID = newID + newReserved - 1;
            freeID = newID + rest;
            if(fromPool > 0)
                return new Pair[] {fromPoolIDs, genPoolIDs};
            return new Pair[] {genPoolIDs};
        }
    }

        // возвращает первый, и резервирует себе еще count id'ков
    public int reserveIDs(int count, SQLSession dataSession, int idType) throws SQLException {
        int freeID = 0;
        try {
            dataSession.startTransaction(DBManager.ID_TIL);

            freeID = (Integer) getGenerateQuery(idType).execute(dataSession).singleValue().get(value) + 1; // замещаем

            QueryBuilder<KeyField, PropertyField> updateQuery = new QueryBuilder<KeyField, PropertyField>(this, MapFact.singleton(key, new DataObject(idType, SystemClass.instance)));
            updateQuery.addProperty(value, new ValueExpr(freeID + count - 1, SystemClass.instance));
            dataSession.updateRecords(new ModifyQuery(this, updateQuery.getQuery()));

            dataSession.commitTransaction();
        } catch (Throwable e) {
            if(e instanceof SQLHandledException && ((SQLHandledException)e).isRepeatableApply()) // update conflict или deadlock или timeout - пробуем еще раз
                return reserveIDs(count, dataSession, idType);
            
            dataSession.rollbackTransaction();
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
