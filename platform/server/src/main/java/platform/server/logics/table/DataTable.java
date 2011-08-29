package platform.server.logics.table;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.classes.IntegerClass;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.*;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.Where;

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

    public void updateStat(SQLSession session) throws SQLException {
        Query<Object, Object> query = new Query<Object, Object>(new ArrayList<Object>());

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
            query.properties.put(prop, GroupExpr.create(new HashMap<Object, KeyExpr>(), one,
                    GroupExpr.create(Collections.singletonMap(0, join.getExpr(prop)), Where.TRUE, Collections.singletonMap(0, new KeyExpr("count"))).getWhere(), GroupType.SUM, new HashMap<Object, Expr>()));
        }

        query.properties.put(0, GroupExpr.create(new HashMap<Object, Expr>(), one, inWhere, GroupType.SUM, new HashMap<Object, Expr>()));
        query.and(Where.TRUE);

        Map<Object, Object> result = BaseUtils.singleValue(query.execute(session));

        DistinctKeys<KeyField> distinctKeys = new DistinctKeys<KeyField>();
        for(KeyField key : keys)
            distinctKeys.add(key, new Stat(BaseUtils.nvl((Integer) result.get(key), 0)));
        statKeys = new StatKeys<KeyField>(new Stat(BaseUtils.nvl((Integer)result.get(0), 0)), distinctKeys);

        statProps = new HashMap<PropertyField, Stat>();
        for(PropertyField prop : properties)
            statProps.put(prop, new Stat(BaseUtils.nvl((Integer)result.get(prop), 0)));
    }
}
