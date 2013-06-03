package lsfusion.server.caches;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.data.Value;

// только с интерфейсом хэширования, нужен в группировках на "стыке" внешнего и внутреннего контекста 
public interface InnerHashContext {

    public abstract int hashInner(HashContext hashContext);

    public abstract ImSet<ParamExpr> getInnerKeys();
    public abstract ImSet<Value> getInnerValues();

    // строит hash с точностью до перестановок
    public int hashValues(HashValues hashValues);
    public BaseUtils.HashComponents<ParamExpr> getComponents(final HashValues hashValues);
}
