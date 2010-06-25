package platform.server.data.expr;

import platform.server.data.type.Type;
import platform.server.classes.ValueClass;

public interface KeyType {

    Type getKeyType(KeyExpr expr);
}
