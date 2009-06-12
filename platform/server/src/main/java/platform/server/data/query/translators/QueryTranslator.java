package platform.server.data.query.translators;

import platform.base.BaseUtils;
import platform.server.data.query.DataJoin;
import platform.server.data.query.Join;
import platform.server.data.query.Context;
import platform.server.data.query.wheres.JoinWhere;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.where.Where;

import java.util.Collection;
import java.util.Map;

public class QueryTranslator extends JoinTranslator<SourceExpr, SourceExpr, Where> {

    public QueryTranslator(Context transContext, Map<KeyExpr, ? extends SourceExpr> joinImplement, Map<ValueExpr, ValueExpr> iValues) {
        super((Map<KeyExpr, SourceExpr>) joinImplement, iValues);

        context = new Context();
        context.fill(keys.values(),false);
        context.fill(values.values(),false);
        for(Collection<DataJoin> level : transContext) // перетранслируем все join'ы
            for(DataJoin join : level)
                context.add(join.translate(this).getContext());
    }

    public Where translate(JoinWhere where) {
        return BaseUtils.nvl(wheres.get(where),where);
    }
    
    public SourceExpr translate(JoinExpr expr) {
        return BaseUtils.nvl(exprs.get(expr),expr);
    }

    public SourceExpr translate(KeyExpr key) {
        return BaseUtils.nvl(keys.get(key),key);
    }

    public <J,U> void retranslate(DataJoin<J,U> join, Join<U> transJoin) {
        wheres.put(join.inJoin,transJoin.getWhere());
        for(Map.Entry<U, JoinExpr<J,U>> joinExpr : join.exprs.entrySet())
            exprs.put(joinExpr.getValue(),transJoin.getExpr(joinExpr.getKey()));
    }
}
