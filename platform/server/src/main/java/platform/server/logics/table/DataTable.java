package platform.server.logics.table;

import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.mapvalue.ImValueMap;
import platform.server.SystemProperties;
import platform.server.classes.DataClass;
import platform.server.classes.IntegerClass;
import platform.server.data.GlobalTable;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.SerializedTable;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.DistinctKeys;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.QueryBuilder;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ReflectionLogicsModule;
import platform.server.session.DataSession;

import java.sql.SQLException;

public abstract class DataTable extends GlobalTable {

    protected DataTable(String name) {
        super(name);
    }

    private StatKeys<KeyField> statKeys = null;
    private ImMap<PropertyField, Stat> statProps = null;

    public StatKeys<KeyField> getStatKeys() {
        if(statKeys!=null)
            return statKeys;
        else
            return SerializedTable.getStatKeys(this);
    }

    public ImMap<PropertyField, Stat> getStatProps() {
        if(statProps!=null)
            return statProps;
        else
            return SerializedTable.getStatProps(this);
    }

    public void calculateStat(ReflectionLogicsModule reflectionLM, DataSession session) throws SQLException {
        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(SetFact.EMPTY());

        if (!SystemProperties.doNotCalculateStats) {
            ValueExpr one = new ValueExpr(1, IntegerClass.instance);

            ImRevMap<KeyField, KeyExpr> mapKeys = getMapKeys();
            platform.server.data.query.Join<PropertyField> join = join(mapKeys);

            Where inWhere = join.getWhere();
            for(KeyField key : keys) {
                ImMap<Object, Expr> map = MapFact.<Object, Expr>singleton(0, mapKeys.get(key));
                query.addProperty(key, GroupExpr.create(MapFact.<Integer, Expr>EMPTY(), one,
                        GroupExpr.create(map, inWhere, map).getWhere(), GroupType.SUM, MapFact.<Integer, Expr>EMPTY()));
            }

            for(PropertyField prop : properties) {
                if (!(prop.type instanceof DataClass && !((DataClass)prop.type).calculateStat()))
                    query.addProperty(prop, GroupExpr.create(MapFact.<Object, KeyExpr>EMPTY(), one,
                            GroupExpr.create(MapFact.singleton(0, join.getExpr(prop)), Where.TRUE, MapFact.singleton(0, new KeyExpr("count"))).getWhere(), GroupType.SUM, MapFact.<Object, Expr>EMPTY()));
            }

            query.addProperty(0, GroupExpr.create(MapFact.<Object, Expr>EMPTY(), one, inWhere, GroupType.SUM, MapFact.<Object, Expr>EMPTY()));
            query.and(Where.TRUE);

            ImMap<Object, Object> result = query.execute(session).singleValue();

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
            QueryBuilder<Object, Object> notNullQuery = new QueryBuilder<Object, Object>(MapFact.<Object, KeyExpr>EMPTYREV());
            for (PropertyField property : properties)
                notNullQuery.addProperty(property, GroupExpr.create(MapFact.<Object, Expr>EMPTY(), one, join.getExpr(property).getWhere(), GroupType.SUM, MapFact.<Object, Expr>EMPTY()));
            ImMap<Object, Object> notNulls = notNullQuery.execute(session).singleValue();
            int sparseColumns = 0;
            for (PropertyField property : properties) {
                DataObject propertyObject = session.getDataObject(reflectionLM.tableColumnSID.read(session, new DataObject(property.name)), reflectionLM.tableColumn.getType());
                int notNull = (Integer) BaseUtils.nvl(notNulls.get(property), 0);
                int total = (Integer) BaseUtils.nvl(result.get(0), 0);
                double perCent = total != 0 ? 100 * (double) notNull / total : 100;
                reflectionLM.notNullQuantityTableColumn.change(notNull, session, propertyObject);
                reflectionLM.percentNotNullTableColumn.change(perCent, session, propertyObject);
                if (perCent < 50) {
                    sparseColumns++;
                }
            }
            reflectionLM.sparseColumnsTable.change(sparseColumns, session, tableObject);
        }
    }

    public void updateStat(ImMap<String, Integer> tableStats, ImMap<String, Integer> keyStats, ImMap<String, Integer> propStats, boolean statDefault) throws SQLException {
        Stat rowStat;
        if (!tableStats.containsKey(name))
            rowStat = Stat.DEFAULT;
        else
            rowStat = new Stat(BaseUtils.nvl(tableStats.get(name), 0));

        ImValueMap<KeyField, Stat> mvDistinctKeys = getTableKeys().mapItValues(); // exception есть
        for(int i=0,size=keys.size();i<size;i++) {
            String keySID = name + "." + keys.get(i).name;
            if (!keyStats.containsKey(keySID))
                mvDistinctKeys.mapValue(i, Stat.DEFAULT);
            else
                mvDistinctKeys.mapValue(i, new Stat(BaseUtils.nvl(keyStats.get(keySID), 0)));
        }
        statKeys = new StatKeys<KeyField>(rowStat, new DistinctKeys<KeyField>(mvDistinctKeys.immutableValue()));

        ImValueMap<PropertyField, Stat> mvUpdateStatProps = properties.mapItValues();
        for(int i=0,size=properties.size();i<size;i++) {
            PropertyField prop = properties.get(i);
            if (prop.type instanceof DataClass && !((DataClass)prop.type).calculateStat())
                mvUpdateStatProps.mapValue(i, ((DataClass)prop.type).getTypeStat().min(rowStat));
            else {
                if (!propStats.containsKey(prop.name))
                    mvUpdateStatProps.mapValue(i, Stat.DEFAULT);
                else
                    mvUpdateStatProps.mapValue(i, new Stat(BaseUtils.nvl((Integer) propStats.get(prop.name), 0)));
            }
        }
        statProps = mvUpdateStatProps.immutableValue();
    }
}
