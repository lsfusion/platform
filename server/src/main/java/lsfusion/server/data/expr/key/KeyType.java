package lsfusion.server.data.expr.key;

import lsfusion.server.data.type.Type;

public interface KeyType {

    Type getKeyType(ParamExpr expr);
}
