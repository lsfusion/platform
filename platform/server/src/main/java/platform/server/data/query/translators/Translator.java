package platform.server.data.query.translators;

import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.wheres.JoinWhere;
import platform.server.data.query.Context;
import platform.server.where.Where;

import java.util.Map;

public interface Translator<T extends SourceExpr,J extends SourceExpr, W extends Where> {

    W translate(JoinWhere where);
    J translate(JoinExpr expr);
    T translate(KeyExpr expr);
    ValueExpr translate(ValueExpr expr);

    <K> Map<K,SourceExpr> translate(Map<K,? extends SourceExpr> map);

    boolean direct();

    Context getContext();
}
