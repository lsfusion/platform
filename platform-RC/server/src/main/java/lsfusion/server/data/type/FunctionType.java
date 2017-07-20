package lsfusion.server.data.type;

import lsfusion.base.BinarySerializable;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.MSSQLDataAdapter;
import lsfusion.server.data.sql.SQLSyntax;

public interface FunctionType extends BinarySerializable {
    String getDB(SQLSyntax syntax, TypeEnvironment typeEnv);

    String getParamFunctionDB(SQLSyntax syntax, TypeEnvironment typeEnv);
}
