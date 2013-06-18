package lsfusion.server.data.expr.formula;

import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.ExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

public interface ExprSource extends ExprType {

    String getSource(int i);

    SQLSyntax getSyntax();

    ExecuteEnvironment getEnv();
}
