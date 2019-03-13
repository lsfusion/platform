package lsfusion.server.data.expr;

import lsfusion.server.base.caches.ParamExpr;
import lsfusion.server.data.type.Type;

public interface KeyType {

    Type getKeyType(ParamExpr expr);
}
