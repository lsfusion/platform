package platform.server.data.expr;

import platform.server.data.type.Type;

public interface KeyType {

    Type getKeyType(KeyExpr expr);
}
