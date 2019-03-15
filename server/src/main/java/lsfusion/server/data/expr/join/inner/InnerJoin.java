package lsfusion.server.data.expr.join.inner;

import lsfusion.server.data.expr.InnerExpr;
import lsfusion.server.data.expr.query.stat.StatType;
import lsfusion.server.data.expr.join.classes.InnerFollows;
import lsfusion.server.data.expr.join.stat.InnerBaseJoin;
import lsfusion.server.data.expr.join.stat.StatKeys;
import lsfusion.server.data.expr.join.stat.WhereJoin;

public interface InnerJoin<K, IJ extends InnerJoin<K, IJ>> extends WhereJoin<K, IJ>, InnerBaseJoin<K> {

    InnerFollows<K> getInnerFollows();

    InnerExpr getInnerExpr(WhereJoin join);

    boolean isValue();

    StatKeys<K> getInnerStatKeys(StatType type);
}
