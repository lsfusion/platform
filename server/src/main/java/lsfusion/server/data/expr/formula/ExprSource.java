package lsfusion.server.data.expr.formula;

import lsfusion.server.data.query.exec.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.syntax.SQLSyntax;

public interface ExprSource extends ExprType {

    String getSource(int i);

    SQLSyntax getSyntax();

    MStaticExecuteEnvironment getMEnv();

    boolean isToString();
}
