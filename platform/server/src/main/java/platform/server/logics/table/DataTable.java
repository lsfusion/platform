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
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    public void calculateStat(BaseLogicsModule LM, DataSession session) throws SQLException {
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

            DataObject tableObject = session.getDataObject(LM.sidToTable.read(session, new DataObject(name)), LM.table.getType());
            LM.rowsTable.execute(BaseUtils.nvl(result.get(0), 0), session, tableObject);

            for (KeyField key : keys) {
                DataObject keyObject = session.getDataObject(LM.sidToTableKey.read(session, new DataObject(name + "." + key.name)), LM.tableKey.getType());
                LM.quantityTableKey.execute(BaseUtils.nvl(result.get(key), 0), session, keyObject);
            }

            for (PropertyField property : properties) {
                DataObject propertyObject = session.getDataObject(LM.sidToTableColumn.read(session, new DataObject(property.name)), LM.tableColumn.getType());
                LM.quantityTableColumn.execute(BaseUtils.nvl(result.get(property), 0), session, propertyObject);
            }
        }
    }

    public void updateStat(BaseLogicsModule LM, DataSession session) throws SQLException {
        boolean statDefault = ("true".equals(System.getProperty("platform.server.logics.donotcalculatestats")));

        Object tableValue = LM.sidToTable.read(session, new DataObject(name));
        Stat rowStat;
        if (tableValue == null || statDefault) {
            rowStat = Stat.DEFAULT;
        } else {
            DataObject tableObject = new DataObject(tableValue, LM.table);
            rowStat = new Stat(BaseUtils.nvl((Integer) LM.rowsTable.read(session, tableObject), 0));
        }

        DistinctKeys<KeyField> distinctKeys = new DistinctKeys<KeyField>();
        for(KeyField key : keys) {
            Object keyValue = LM.sidToTableKey.read(session, new DataObject(name + "." + key.name));
            if (keyValue == null || statDefault) {
                distinctKeys.add(key, Stat.DEFAULT);
            } else {
                DataObject keyObject = new DataObject(keyValue, LM.tableKey);
                distinctKeys.add(key, new Stat(BaseUtils.nvl((Integer) LM.quantityTableKey.read(session, keyObject), 0)));
            }
        }
        statKeys = new StatKeys<KeyField>(rowStat, distinctKeys);

        statProps = new HashMap<PropertyField, Stat>();
        for(PropertyField prop : properties) {
            if (prop.type instanceof DataClass && !((DataClass)prop.type).calculateStat())
                statProps.put(prop, ((DataClass)prop.type).getTypeStat().min(rowStat));
            else {
                Object propertyValue = LM.sidToTableColumn.read(session, new DataObject(prop.name));
                if (propertyValue == null || statDefault) {
                    statProps.put(prop, Stat.DEFAULT);
                } else {
                    DataObject propertyObject = new DataObject(propertyValue, LM.tableColumn);
                    statProps.put(prop, new Stat(BaseUtils.nvl((Integer) LM.quantityTableColumn.read(session, propertyObject), 0)));
                }
            }
        }
    }
}
