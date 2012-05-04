package platform.server.caches;

import platform.base.*;
import platform.server.caches.hash.*;
import platform.server.data.expr.KeyExpr;

import java.util.*;

// только с интерфейсом хэширования, нужен в группировках на "стыке" внешнего и внутреннего контекста 
public interface InnerHashContext {

    public abstract int hashInner(HashContext hashContext);

    public abstract QuickSet<KeyExpr> getInnerKeys();

    // строит hash с точностью до перестановок
    public int hashValues(HashValues hashValues);
    public BaseUtils.HashComponents<KeyExpr> getComponents(final HashValues hashValues);
}
