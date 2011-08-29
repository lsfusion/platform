package platform.server.data.query.innerjoins;

import platform.server.caches.hash.HashContext;
import platform.server.caches.AbstractOuterContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.translator.MapTranslate;
import platform.server.data.query.SourceJoin;
import platform.base.TwinImmutableInterface;

import java.util.Set;

public class GroupStatKeys<K extends BaseExpr> extends AbstractOuterContext<GroupStatKeys<K>> implements StatInterface<GroupStatKeys<K>> {

    private final StatKeys<K> groups;
    public GroupStatKeys(StatKeys<K> groups) {
        this.groups = groups;
    }

    public int hashOuter(HashContext hashContext) {
        return StatKeys.hashOuter(groups, hashContext);
    }

    public boolean twins(TwinImmutableInterface o) {
        return groups.equals(((GroupStatKeys) o).groups);
    }

    public GroupStatKeys<K> translateOuter(MapTranslate translator) {
        return new GroupStatKeys<K>(StatKeys.translateOuter(groups, translator));
    }

    public SourceJoin[] getEnum() {
        throw new RuntimeException("not supported");
    }

    @Override
    public <K extends BaseExpr> StatKeys<K> getStatKeys(Set<K> groups, KeyStat keyStat) {
        return (StatKeys<K>) this.groups; //assert что совпадают с enough
    }
}
