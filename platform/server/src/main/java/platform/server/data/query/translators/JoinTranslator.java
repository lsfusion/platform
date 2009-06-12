package platform.server.data.query.translators;

import platform.base.BaseUtils;
import platform.server.data.query.Context;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.wheres.JoinWhere;
import platform.server.where.Where;

import java.util.HashMap;
import java.util.Map;

public abstract class JoinTranslator<T extends SourceExpr,J extends SourceExpr,W extends Where> implements Translator<T,J,W> {

    public Context context;
    public Context getContext() {
        return context;
    }

    protected final Map<JoinWhere,W> wheres = new HashMap<JoinWhere, W>();
    public final Map<KeyExpr,T> keys;
    protected final Map<JoinExpr, J> exprs;
    // какой есть - какой нужен
    public final Map<ValueExpr,ValueExpr> values;

    public JoinTranslator() {
        keys = new HashMap<KeyExpr,T>();
        exprs = new HashMap<JoinExpr, J>();
        values = new HashMap<ValueExpr, ValueExpr>();
    }

    public JoinTranslator(Map<KeyExpr,T> iKeys,Map<ValueExpr,ValueExpr> iValues) {
        keys = iKeys;
        exprs = new HashMap<JoinExpr, J>();
        values = iValues;
    }

    boolean direct = false;
    public boolean direct() {
        return direct;
    }

    public ValueExpr translate(ValueExpr expr) {
        return BaseUtils.nvl(values.get(expr),expr);
    }

    public int hashCode() {
        return exprs.hashCode()*31+wheres.hashCode();
    }

    public boolean equals(Object obj) {
        return obj==this || (obj instanceof JoinTranslator && exprs.equals(((JoinTranslator)obj).exprs) && wheres.equals(((JoinTranslator)obj).wheres));
    }

    public <K> Map<K, SourceExpr> translate(Map<K, ? extends SourceExpr> map) {
        Map<K,SourceExpr> transMap = new HashMap<K,SourceExpr>();
        for(Map.Entry<K,? extends SourceExpr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translate(this));
        return transMap;
    }
}
