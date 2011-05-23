package platform.server.data.query.innerjoins;

import platform.server.caches.hash.HashContext;
import platform.server.caches.AbstractOuterContext;
import platform.server.data.translator.MapTranslate;
import platform.server.data.query.SourceJoin;
import platform.server.data.expr.KeyExpr;
import platform.base.TwinImmutableInterface;

import java.util.Set;

public class EnoughKeys extends AbstractOuterContext<EnoughKeys> implements GroupJoinSet<EnoughKeys> {

    private final Set<KeyExpr> keys;
    public EnoughKeys(Set<KeyExpr> keys) {
        this.keys = keys;
    }

    public int hashOuter(HashContext hashContext) {
        int hash = 0;
        for(KeyExpr key : keys)
            hash += hashContext.keys.hash(key);
        return hash;
    }

    public boolean twins(TwinImmutableInterface o) {
        return keys.equals(((EnoughKeys)o).keys);
    }

    public int immutableHashCode() {
        return keys.hashCode();
    }

    public EnoughKeys translateOuter(MapTranslate translator) {
        return new EnoughKeys(translator.translateKeys(keys));
    }

    public SourceJoin[] getEnum() {
        throw new RuntimeException("not supported");
    }

    public Set<KeyExpr> insufficientKeys(Set<KeyExpr> enough) {
        return this.keys;
    }
}
