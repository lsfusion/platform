package lsfusion.server.data.expr.formula;

import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.type.Type;

public interface ExprSource {

    int getExprCount();

    Expr getExpr(int i);

    Type getSelfType(int i);

    Type getType(int i, KeyType keyType);

    String getSource(int i, CompileSource compile);
}
