package platform.server.logics.table;

import platform.base.BaseUtils;
import platform.server.classes.DataClass;
import platform.server.classes.IntegerClass;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.*;
import platform.server.data.query.Query;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ReflectionLogicsModule;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public abstract class DataTable extends GlobalTable {

    protected DataTable(String name) {
        super(name);
    }

    private StatKeys<KeyField> statKeys = null;
    private Map<PropertyField, Stat> statProps = null;

    public StatKeys<KeyField> getStatKeys() {
        if(statKeys!=null)
            return statKeys;
        else
            return SerializedTable.getStatKeys(this);
    }

    public Map<PropertyField, Stat> getStatProps() {
        if(statProps!=null)
            return statProps;
        else
            return SerializedTable.getStatProps(this);
    }

    public void calculateStat(ReflectionLogicsModule reflectionLM, DataSession session) throws SQLException {
        Query<Object, Object> query = new Query<Object, Object>(new ArrayList<Object>());

        if (!("true".equals(System.getProperty("platform.server.logics.donotcalculatestats")))) {
            ValueExpr one = new ValueExpr(1, IntegerClass.instance);

            Map<KeyField, KeyExpr> mapKeys = KeyExpr.getMapKeys(keys);
            platform.server.data.query.Join<PropertyField> join = join(mapKeys);

            Where inWhere = join.getWhere();
            for(KeyField key : keys) {
                Map<Object, Expr> map = Collections.<Object, Expr>singletonMap(0, mapKeys.get(key));
                query.properties.put(key, GroupExpr.create(new HashMap<Integer, Expr>(), one,
                        GroupExpr.create(map, inWhere, map).getWhere(), GroupType.SUM, new HashMap<Integer, Expr>()));
            }

            for(PropertyField prop : properties) {
                if (!(prop.type instanceof DataClass && !((DataClass)prop.type).calculateStat()))
                    query.properties.put(prop, GroupExpr.create(new HashMap<Object, KeyExpr>(), one,
                            GroupExpr.create(Collections.singletonMap(0, join.getExpr(prop)), Where.TRUE, Collections.singletonMap(0, new KeyExpr("count"))).getWhere(), GroupType.SUM, new HashMap<Object, Expr>()));
            }

            query.properties.put(0, GroupExpr.create(new HashMap<Object, Expr>(), one, inWhere, GroupType.SUM, new HashMap<Object, Expr>()));
            query.and(Where.TRUE);

            Map<Object, Object> result = BaseUtils.singleValue(query.execute(session));

            DataObject tableObject = session.getDataObject(reflectionLM.tableSID.read(session, new DataObject(name)), reflectionLM.table.getType());
            reflectionLM.rowsTable.change(BaseUtils.nvl(result.get(0), 0), session, tableObject);

            for (KeyField key : keys) {
                DataObject keyObject = session.getDataObject(reflectionLM.tableKeySID.read(session, new DataObject(name + "." + key.name)), reflectionLM.tableKey.getType());
                reflectionLM.quantityTableKey.change(BaseUtils.nvl(result.get(key), 0), session, keyObject);
            }

            for (PropertyField property : properties) {
                DataObject propertyObject = session.getDataObject(reflectionLM.tableColumnSID.read(session, new DataObject(property.name)), reflectionLM.tableColumn.getType());
                reflectionLM.quantityTableColumn.change(BaseUtils.nvl(result.get(property), 0), session, propertyObject);
            }

            // не null значения и разреженность колонок
            Query<Object, Object> notNullQuery = new Query<Object, Object>(new HashMap<Object, KeyExpr>());
            for (PropertyField property : properties) {
                Expr exprQuant = GroupExpr.create(new HashMap<Object, Expr>(), one, join.getExpr(property).getWhere(), GroupType.SUM, new HashMap<Object, Expr>());
                notNullQuery.getWhere().or(exprQuant.getWhere());
                notNullQuery.properties.put(property, exprQuant);
            }
            Map<Map<Object, Object>, Map<Object, Object>> notNullResult = notNullQuery.execute(session);
            if (notNullResult.size() != 0){
                Map<Object, Object> notNulls = BaseUtils.singleValue(notNullResult);
                int sparseColumns = 0;
                for (PropertyField property : properties) {
                    DataObject propertyObject = session.getDataObject(reflectionLM.tableColumnSID.read(session, new DataObject(property.name)), reflectionLM.tableColumn.getType());
                    int notNull = (Integer) BaseUtils.nvl(notNulls.get(property), 0);
                    int total = (Integer) BaseUtils.nvl(result.get(0), 0);
                    double perCent = total != 0 ? 100 * (double) notNull / total : 100;
                    reflectionLM.notNullQuantityTableColumn.change(notNull, session, propertyObject);
                    reflectionLM.perсentNotNullTableColumn.change(perCent, session, propertyObject);
                    if (perCent < 50) {
                        sparseColumns++;
                    }
                }
                reflectionLM.sparseColumnsTable.change(sparseColumns, session, tableObject);
            }
        }
    }

    public void updateStat(ReflectionLogicsModule reflectionLM, DataSession session, boolean statDefault) throws SQLException {
        Object tableValue;
        Stat rowStat;
        if (statDefault || (tableValue = reflectionLM.tableSID.read(session, new DataObject(name))) == null) {
            rowStat = Stat.DEFAULT;
        } else {
            DataObject tableObject = new DataObject(tableValue, reflectionLM.table);
            rowStat = new Stat(BaseUtils.nvl((Integer) reflectionLM.rowsTable.read(session, tableObject), 0));
        }

        DistinctKeys<KeyField> distinctKeys = new DistinctKeys<KeyField>();
        for(KeyField key : keys) {
            Object keyValue;
            if (statDefault || (keyValue = reflectionLM.tableKeySID.read(session, new DataObject(name + "." + key.name))) == null) {
                distinctKeys.add(key, Stat.DEFAULT);
            } else {
                DataObject keyObject = new DataObject(keyValue, reflectionLM.tableKey);
                distinctKeys.add(key, new Stat(BaseUtils.nvl((Integer) reflectionLM.quantityTableKey.read(session, keyObject), 0)));
            }
        }
        statKeys = new StatKeys<KeyField>(rowStat, distinctKeys);

        Map<PropertyField, Stat> updateStatProps = new HashMap<PropertyField, Stat>();
        for(PropertyField prop : properties) {
            if (prop.type instanceof DataClass && !((DataClass)prop.type).calculateStat())
                updateStatProps.put(prop, ((DataClass)prop.type).getTypeStat().min(rowStat));
            else {
                Object propertyValue;
                if (statDefault || (propertyValue = reflectionLM.tableColumnSID.read(session, new DataObject(prop.name))) == null) {
                    updateStatProps.put(prop, Stat.DEFAULT);
                } else {
                    DataObject propertyObject = new DataObject(propertyValue, reflectionLM.tableColumn);
                    updateStatProps.put(prop, new Stat(BaseUtils.nvl((Integer) reflectionLM.quantityTableColumn.read(session, propertyObject), 0)));
                }
            }
        }
        statProps = updateStatProps;
    }
}
