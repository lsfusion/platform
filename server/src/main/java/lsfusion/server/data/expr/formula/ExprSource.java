package lsfusion.server.data.expr.formula;

import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.query.StaticExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;

public interface ExprSource extends ExprType {

    String getSource(int i);

    SQLSyntax getSyntax();

    MStaticExecuteEnvironment getMEnv();
}
