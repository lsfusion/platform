package lsfusion.server.data.expr.query;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.where.Where;

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
