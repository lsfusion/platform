package platform.server.data.query.innerjoins;

import platform.server.caches.OuterContext;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.KeyStat;
import platform.server.data.expr.query.StatKeys;

import java.util.Map;
import java.util.Set;

public interface GroupJoinSet<This extends GroupJoinSet> extends OuterContext<This> {

    StatKeys<KeyExpr> getStatKeys(Set<KeyExpr> keys);
}
