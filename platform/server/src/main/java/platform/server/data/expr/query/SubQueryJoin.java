package platform.server.data.expr.query;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

public class SubQueryJoin extends QueryJoin<KeyExpr, Where, SubQueryJoin, SubQueryJoin.QueryOuterContext> {

    public SubQueryJoin(ImSet<KeyExpr> keys, ImSet<Value> values, Where inner, ImMap<KeyExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    public static class QueryOuterContext extends QueryJoin.QueryOuterContext<KeyExpr, Where, SubQueryJoin, SubQueryJoin.QueryOuterContext> {
        public QueryOuterContext(SubQueryJoin thisObj) {
            super(thisObj);
        }

        public SubQueryJoin translateThis(MapTranslate translator) {
            return new SubQueryJoin(thisObj, translator);
        }
    }
    protected QueryOuterContext createOuterContext() {
        return new QueryOuterContext(this);
    }

    protected SubQueryJoin createThis(ImSet<KeyExpr> keys, ImSet<Value> values, Where query, ImMap<KeyExpr, BaseExpr> group) {
        return new SubQueryJoin(keys, values, query, group);
    }

    private SubQueryJoin(SubQueryJoin partitionJoin, MapTranslate translator) {
        super(partitionJoin, translator);
    }

    public StatKeys<KeyExpr> getStatKeys() {
        return query.getFullStatKeys(keys);
    }

    // кэшить
    public StatKeys<KeyExpr> getStatKeys(KeyStat keyStat) {
        return getStatKeys();
    }

    public Where getWhere() {
        return query;
    }
}
