package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.GlobalInteger;
import platform.base.GlobalObject;
import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashMapKeys;
import platform.server.caches.hash.HashValues;

public abstract class AbstractInnerHashContext extends AbstractHashContext<HashValues> implements InnerHashContext {

    // строит hash с точностью до перестановок
    public int hashValues(HashValues hashValues) {
        return aspectHash(hashValues);
    }

    protected int hash(HashValues hashValues) {
        return getComponents(hashValues).hash;
    }

    public boolean twins(TwinImmutableObject o) {
        throw new UnsupportedOperationException();
    }

    public int immutableHashCode() {
        throw new UnsupportedOperationException();
    }

    protected HashValues aspectContextHash(HashValues hash) {
        return hash.filterValues(getInnerValues());
    }

    private final static GetValue<GlobalInteger, ParamExpr> getKeyClasses = new GetValue<GlobalInteger, ParamExpr>() {
        public GlobalInteger getMapValue(ParamExpr value) {
            return value.getKeyClass();
        }};
    public BaseUtils.HashComponents<ParamExpr> getComponents(final HashValues hashValues) {
        return BaseUtils.getComponents(new BaseUtils.HashInterface<ParamExpr, GlobalInteger>() {

                public ImMap<ParamExpr, GlobalInteger> getParams() {
                    return getInnerKeys().mapValues(getKeyClasses);
                }

                public int hashParams(ImMap<ParamExpr, ? extends GlobalObject> map) {
                    return hashInner(HashContext.create(HashMapKeys.create(map), hashValues));
                }
            });
    }

    public boolean equals(Object obj) {
        throw new UnsupportedOperationException();
    }

    public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
