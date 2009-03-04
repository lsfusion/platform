package platform.server.data.query;

import platform.server.data.query.exprs.CaseExpr;
import platform.server.data.query.exprs.ObjectExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.wheres.JoinWhere;
import platform.server.where.Where;

import java.util.HashMap;
import java.util.Map;

public class ExprTranslator implements Translator {

    private Map<JoinWhere,Where> wheres = new HashMap<JoinWhere, Where>();
//    Map<DataWhere,Where> Wheres = new HashMap<DataWhere, Where>();
    private Map<ObjectExpr, SourceExpr> exprs = new HashMap<ObjectExpr, SourceExpr>();

    void put(JoinWhere where, Where to) {
        wheres.put(where, to);
    }

    void put(ObjectExpr expr, SourceExpr to) {
        exprs.put(expr, to);
    }

    void putAll(Map<? extends ObjectExpr,? extends SourceExpr> map) {
        exprs.putAll(map);
    }

    void putAll(ExprTranslator translator) {
        exprs.putAll(translator.exprs);
        wheres.putAll(translator.wheres);
    }

    boolean direct = false;
    public boolean direct() {
        return direct;
    }
    boolean hasCases() {
        for(SourceExpr expr : exprs.values())
            if(expr instanceof CaseExpr)
                return true;
        return false;
    }

    public Where translate(JoinWhere where) {
        Where result = wheres.get(where);
        if(result==null) result = where;
        return result;
    }

    public SourceExpr translate(ObjectExpr expr) {
        SourceExpr result = exprs.get(expr);
        if(result==null) result = expr;
        return result;
    }

    public int hashCode() {
        return exprs.hashCode()*31+wheres.hashCode();
    }

    public boolean equals(Object obj) {
        return obj==this || (obj instanceof ExprTranslator && exprs.equals(((ExprTranslator)obj).exprs) && wheres.equals(((ExprTranslator)obj).wheres));
    }
}
