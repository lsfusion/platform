package platform.server.data.expr.formula;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public interface ExprSource {

    int getExprCount();

    Expr getExpr(int i);

    Type getType(int i, KeyType keyType);

    String getSource(int i, CompileSource compile);
}
