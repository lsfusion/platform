package platform.server.data.query.translators;

import platform.server.data.query.DataJoin;
import platform.server.data.query.Context;
import platform.server.data.query.wheres.JoinWhere;
import platform.server.data.query.exprs.JoinExpr;

import java.util.Map;

public class MergeTranslator extends DirectJoinTranslator {

    public MergeTranslator(Context mergeJoins) {
        context = new Context(mergeJoins, this);
    }

    public <J,U,MU> void retranslate(DataJoin<J,U> join,DataJoin<?,MU> transJoin, Map<U,MU> mapProps) {
        if(join.inJoin!=null) wheres.put(join.inJoin,(JoinWhere)transJoin.getWhere());
        for(Map.Entry<U, JoinExpr<J,U>> joinExpr : join.exprs.entrySet()) // assertion что не null
            exprs.put(joinExpr.getValue(), (JoinExpr) transJoin.getExpr(mapProps.get(joinExpr.getKey())));
    }
}
