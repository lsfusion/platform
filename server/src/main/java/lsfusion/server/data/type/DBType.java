package lsfusion.server.data.type;

import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;

public interface DBType {
    String getDBString(SQLSyntax syntax, TypeEnvironment typeEnv);
}
