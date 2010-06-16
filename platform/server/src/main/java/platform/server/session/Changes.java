package platform.server.session;

import platform.server.caches.AbstractMapValues;
import platform.server.caches.GenericImmutable;
import platform.server.caches.GenericLazy;
import platform.server.caches.hash.HashValues;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.MapValuesTranslate;

import java.util.Set;

@GenericImmutable
public abstract class Changes<U extends Changes<U>> extends AbstractMapValues<U> {

    public SessionChanges session;

    public boolean hasChanges() {
        return session.hasChanges() || modifyUsed();
    }

    public Changes() {
        session = SessionChanges.EMPTY;
    }

    protected Changes(Changes<U> changes, MapValuesTranslate mapValues) {
        session = changes.session.translate(mapValues);
    }

    public Changes(Modifier<U> modifier) { // можно так как SessionChanges Immutable
        session = modifier.getSession();
    }

    // весь этот огород, для того чтобы если даже разные классы, но нету изменений, все равно давать equals и использовать одни кэши
    public boolean modifyUsed() {
        return false;
    }

    protected boolean modifyEquals(U changes) {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Changes && session.equals(((Changes) o).session))) return false;

        if(getClass()==o.getClass())
            return modifyEquals((U)o);

        return !modifyUsed() && !((Changes)o).modifyUsed();
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
