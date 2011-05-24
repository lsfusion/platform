package platform.server.data.query.innerjoins;

import platform.server.caches.OuterContext;
import platform.server.data.expr.KeyExpr;

import java.util.Set;

public interface GroupJoinSet<This extends GroupJoinSet> extends OuterContext<This> {

    Set<KeyExpr> insufficientKeys(Set<KeyExpr> keys);
}
