package lsfusion.server.caches;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.data.Value;

// только с интерфейсом хэширования, нужен в группировках на "стыке" внешнего и внутреннего контекста 
public interface InnerHashContext {

    int hashInner(HashContext hashContext);

    ImSet<ParamExpr> getInnerKeys();
    ImSet<Value> getInnerValues();

    // строит hash с точностью до перестановок
    int hashValues(HashValues hashValues);
    BaseUtils.HashComponents<ParamExpr> getComponents(final HashValues hashValues);
}
