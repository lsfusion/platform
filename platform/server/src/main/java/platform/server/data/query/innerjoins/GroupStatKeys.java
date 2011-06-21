package platform.server.data.query.innerjoins;

import platform.server.caches.hash.HashContext;
import platform.server.caches.AbstractOuterContext;
import platform.server.data.expr.query.StatKeys;
import platform.server.data.translator.MapTranslate;
import platform.server.data.query.SourceJoin;
import platform.server.data.expr.KeyExpr;
import platform.base.TwinImmutableInterface;

import java.util.Set;

public class GroupStatKeys extends AbstractOuterContext<GroupStatKeys> implements GroupJoinSet<GroupStatKeys> {

    private final StatKeys<KeyExpr> keys;
    public GroupStatKeys(StatKeys<KeyExpr> keys) {
        this.keys = keys;
    }

    public int hashOuter(HashContext hashContext) {
        int hash = 0;
        for(int i=0;i<keys.size;i++)
            hash += hashContext.keys.hash(keys.getKey(i)) ^ keys.getValue(i).hashCode();
        return hash;
    }

    public boolean twins(TwinImmutableInterface o) {
        return keys.equals(((GroupStatKeys) o).keys);
    }

    public GroupStatKeys translateOuter(MapTranslate translator) {
        StatKeys<KeyExpr> transKeys = new StatKeys<KeyExpr>();
        for(int i=0;i<keys.size;i++)
            transKeys.add(translator.translate(keys.getKey(i)), keys.getValue(i));
        return new GroupStatKeys(transKeys);
    }

    public SourceJoin[] getEnum() {
        throw new RuntimeException("not supported");
    }

    public StatKeys<KeyExpr> getStatKeys(Set<KeyExpr> enough) {
        return keys;
    }
}
