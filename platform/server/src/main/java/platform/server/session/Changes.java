package platform.server.session;

import platform.server.caches.hash.HashValues;
import platform.server.caches.AbstractMapValues;
import platform.server.caches.GenericImmutable;
import platform.server.caches.GenericLazy;
import platform.server.data.expr.ValueExpr;

import java.util.Set;
import java.util.Map;

@GenericImmutable
public abstract class Changes<U extends Changes<U>> extends AbstractMapValues<U> {

    public SessionChanges session;

    public boolean hasChanges() {
        return session.hasChanges();
    }

    public Changes() {
        session = SessionChanges.EMPTY;
    }

    protected Changes(Changes<U> changes, Map<ValueExpr,ValueExpr> mapValues) {
        session = changes.session.translate(mapValues);
    }

    public Changes(Modifier<U> modifier) { // можно так как SessionChanges Immutable
        session = modifier.getSession();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Changes && session.equals(((Changes) o).session);
    }

    protected Changes(U changes, SessionChanges merge) {
        session = changes.session.add(merge);
    }
    public abstract U addChanges(SessionChanges changes);
    
    protected Changes(U changes, U merge) {
        session = changes.session.add(merge.session);
    }
    public abstract U add(U changes);

    @GenericLazy
    public int hashValues(HashValues hashValues) {
        return session.hashValues(hashValues);
    }

    @GenericLazy
    public Set<ValueExpr> getValues() {
        return session.getValues();
    }
}
