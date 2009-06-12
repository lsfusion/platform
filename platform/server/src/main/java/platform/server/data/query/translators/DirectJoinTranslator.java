package platform.server.data.query.translators;

import platform.base.BaseUtils;
import platform.server.data.query.DataJoin;
import platform.server.data.query.Context;
import platform.server.data.query.wheres.JoinWhere;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.ValueExpr;

import java.util.HashMap;
import java.util.Map;

public abstract class DirectJoinTranslator extends JoinTranslator<KeyExpr,JoinExpr,JoinWhere> implements DirectTranslator {

    protected DirectJoinTranslator() {
    }

    protected DirectJoinTranslator(Map<KeyExpr, KeyExpr> iKeys, Map<ValueExpr, ValueExpr> iValues) {
        super(iKeys, iValues);
    }

    public <K> Map<K, AndExpr> translateAnd(Map<K, AndExpr> map) {
        Map<K,AndExpr> transMap = new HashMap<K, AndExpr>();
        for(Map.Entry<K,AndExpr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateAnd(this));
        return transMap;
    }

    public JoinWhere translate(JoinWhere where) {
        return BaseUtils.nvl(wheres.get(where),where);
    }

    public JoinExpr translate(JoinExpr expr) {
        return BaseUtils.nvl(exprs.get(expr),expr);
    }

    public KeyExpr translate(KeyExpr expr) {
        return BaseUtils.nvl(keys.get(expr),expr);
    }

    public <J,U> void retranslate(DataJoin<J,U> join, DataJoin<?,U> transJoin) {
        if(join.inJoin!=null) wheres.put(join.inJoin,(JoinWhere) transJoin.getWhere()); // assertion'ы что там тоже не null'ы
        for(Map.Entry<U, JoinExpr<J,U>> joinExpr : join.exprs.entrySet())
            exprs.put(joinExpr.getValue(), (JoinExpr) transJoin.getExpr(joinExpr.getKey()));
    }
}
