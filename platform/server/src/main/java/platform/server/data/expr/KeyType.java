package platform.server.data.expr;

import platform.server.caches.ParamExpr;
import platform.server.data.type.Type;

public interface KeyType {

    Type getKeyType(ParamExpr expr);
}
