package lsfusion.server.data.expr.join.inner;

import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.expr.join.classes.InnerFollows;
import lsfusion.server.data.expr.join.where.WhereJoin;
import lsfusion.server.data.stat.StatKeys;
import lsfusion.server.data.stat.StatType;

public interface InnerJoin<K, IJ extends InnerJoin<K, IJ>> extends WhereJoin<K, IJ>, InnerBaseJoin<K> {

    InnerFollows<K> getInnerFollows();

    InnerExpr getInnerExpr(WhereJoin join);

    boolean isValue();

    StatKeys<K> getInnerStatKeys(StatType type);
}
