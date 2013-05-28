package platform.server.data.query;

import platform.server.data.expr.InnerExpr;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.WhereJoin;

public interface InnerJoin<K, IJ extends InnerJoin<K, IJ>> extends WhereJoin<K, IJ>, InnerBaseJoin<K> {

    InnerExpr getInnerExpr(WhereJoin join);
}
