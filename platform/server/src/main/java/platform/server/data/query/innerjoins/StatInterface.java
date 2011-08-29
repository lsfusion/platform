package platform.server.data.query.innerjoins;

import platform.server.caches.OuterContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.KeyType;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;

import java.util.Set;

public interface StatInterface<This extends StatInterface> extends OuterContext<This> {

    <K extends BaseExpr> StatKeys<K> getStatKeys(Set<K> groups, KeyStat keyStat);
}
